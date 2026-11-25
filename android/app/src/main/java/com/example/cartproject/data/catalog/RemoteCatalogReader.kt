package com.example.cartproject.data.catalog

import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.CatalogDeckContent
import com.example.cartproject.domain.decks.CatalogDeckId
import com.example.cartproject.domain.decks.CatalogDeckSummary
import com.example.cartproject.domain.decks.CatalogReader
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.serialization.ContentConvertException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull

/** Читает публичный каталог и переводит HTTP-ответы в доменные результаты. */
internal class RemoteCatalogReader(private val client: HttpClient, private val origin: Url) :
    CatalogReader {
    override suspend fun search(query: String): Outcome<List<CatalogDeckSummary>> =
        fetch(query = query, map = CatalogListDto::toDomain)

    override suspend fun readDeck(id: CatalogDeckId): Outcome<CatalogDeckContent> =
        fetch(deckId = id) { dto: CatalogDetailDto -> dto.toDomain(id) }

    private suspend inline fun <reified D : Any, T> fetch(
        query: String? = null,
        deckId: CatalogDeckId? = null,
        crossinline map: (D) -> Outcome<T>,
    ): Outcome<T> = withTimeoutOrNull(CATALOG_TIMEOUT_MILLIS) {
        try {
            val response = client.get(origin) {
                url {
                    appendPathSegments("api", "v1", "decks")
                    if (deckId != null) appendPathSegments(deckId.value.toString())
                    if (!query.isNullOrEmpty()) parameters.append("q", query)
                }
                accept(ContentType.Application.Json)
            }
            if (response.status == HttpStatusCode.OK) {
                response.catalogBody<D>()?.let(map) ?: Outcome.Failure(AppError.InvalidCatalogData)
            } else {
                Outcome.Failure(response.catalogError(deckId != null))
            }
        } catch (failure: IOException) {
            currentCoroutineContext().ensureActive()
            val error = when (failure) {
                is HttpRequestTimeoutException,
                is ConnectTimeoutException,
                is SocketTimeoutException,
                -> AppError.CatalogTimeout
                else -> AppError.Network
            }
            Outcome.Failure(error)
        }
    } ?: Outcome.Failure(AppError.CatalogTimeout)

    private suspend fun HttpResponse.catalogError(isDeckLookup: Boolean): AppError {
        val code = status.value
        return when {
            code == 404 && isDeckLookup &&
                catalogBody<CatalogErrorResponseDto>()?.error?.code == "deck_not_found" ->
                AppError.DeckNotFound
            code == 408 || code == 504 -> AppError.CatalogTimeout
            code == 429 || code in 500..599 -> AppError.CatalogUnavailable
            code in 400..499 -> AppError.CatalogRequestRejected
            else -> AppError.InvalidCatalogData
        }
    }
}

private suspend inline fun <reified T : Any> HttpResponse.catalogBody(): T? = try {
    body<T>()
} catch (_: ContentConvertException) {
    null
} catch (_: NoTransformationFoundException) {
    null
}
