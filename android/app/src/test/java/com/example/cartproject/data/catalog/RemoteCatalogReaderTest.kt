package com.example.cartproject.data.catalog

import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.CatalogDeckId
import com.example.cartproject.domain.decks.CatalogDeckSummary
import com.example.cartproject.domain.decks.SearchCatalog
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.headersOf
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Проверяет запросы, ответы и отмену клиента через MockEngine. */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteCatalogReaderTest {
    private val origin = Url("https://catalog.example")
    private val uuid = "356f61bb-a918-4e0f-8cd8-369c1f0e318a"
    private val deckId = CatalogDeckId(UUID.fromString(uuid))
    private val summary = """{"id":"$uuid","title":"Столицы","description":"","card_count":1}"""

    /** Поиск передаёт Unicode и знаки запроса как один параметр без авторизации редактора. */
    @Test
    fun passesEncodedSearchAndParsesCatalogList() = runTest {
        val query = "Столицы %_+&"
        val response = """{"items":[$summary],"extra":"ignored"}"""
        withReader({
            assertEquals(HttpMethod.Get, it.method)
            assertEquals(URLProtocol.HTTPS, it.url.protocol)
            assertEquals(origin.host, it.url.host)
            assertEquals("/api/v1/decks", it.url.encodedPath)
            assertEquals(setOf("q"), it.url.parameters.names())
            assertEquals(query, it.url.parameters["q"])
            assertTrue(it.url.toString().contains("%25"))
            assertTrue(it.url.toString().contains("%26"))
            assertNull(it.headers["X-Editor-Key"])
            assertNull(it.headers[HttpHeaders.Authorization])
            assertTrue(it.headers[HttpHeaders.Accept].orEmpty().contains("application/json"))
            json(response)
        }) { reader ->
            val expected = listOf(CatalogDeckSummary(deckId, "Столицы", "", 1))
            assertEquals(Outcome.Success(expected), SearchCatalog(reader)("  $query  "))
        }
    }

    /** Пустой поиск использует URL каталога без параметров. */
    @Test
    fun emptyQueryProducesEmptyCatalogNormally() = runTest {
        withReader({
            assertTrue(it.url.parameters.names().isEmpty())
            json("""{"items":[]}""")
        }) { reader ->
            assertEquals(Outcome.Success(emptyList<CatalogDeckSummary>()), reader.search(""))
        }
    }

    /** Подробности читаются одним запросом и включают все карточки. */
    @Test
    fun readsFullDeckInOneRequest() = runTest {
        var calls = 0
        val card = """
            {"id":"184c9b9e-7ca9-4440-ac60-aa682141ee33",
             "question":"Вопрос\nДеталь","answer":"Ответ"}
        """.trimIndent()
        val detail = """{"id":"$uuid","title":"Столицы","description":"","cards":[$card]}"""
        withReader({
            calls++
            assertEquals(HttpMethod.Get, it.method)
            assertEquals("/api/v1/decks/$uuid", it.url.encodedPath)
            json(detail)
        }) { reader ->
            val result = (reader.readDeck(deckId) as Outcome.Success).value
            assertEquals(deckId, result.id)
            assertEquals("Вопрос\nДеталь", result.cards.single().question)
            assertEquals(1, calls)
        }
    }

    /** Отсутствие набора отличается от отсутствия маршрута. */
    @Test
    fun distinguishesDeckNotFoundFromRouteNotFound() = runTest {
        var code = "deck_not_found"
        withReader({
            json(
                """{"error":{"code":"$code","message":"Текст сервера"}}""",
                HttpStatusCode.NotFound,
            )
        }) { reader ->
            assertEquals(Outcome.Failure(AppError.DeckNotFound), reader.readDeck(deckId))
            code = "route_not_found"
            assertEquals(Outcome.Failure(AppError.CatalogRequestRejected), reader.readDeck(deckId))
            assertEquals(Outcome.Failure(AppError.CatalogRequestRejected), reader.search(""))
        }
    }

    /** HTTP-ошибки обрабатываются и при ответе прокси без JSON. */
    @Test
    fun mapsServerStatusesWithoutServerText() = runTest {
        var status = HttpStatusCode.ServiceUnavailable
        withReader({ respond("proxy", status) }) { reader ->
            val cases = listOf(
                HttpStatusCode.ServiceUnavailable to AppError.CatalogUnavailable,
                HttpStatusCode.BadGateway to AppError.CatalogUnavailable,
                HttpStatusCode.InternalServerError to AppError.CatalogUnavailable,
                HttpStatusCode.TooManyRequests to AppError.CatalogUnavailable,
                HttpStatusCode.GatewayTimeout to AppError.CatalogTimeout,
                HttpStatusCode.RequestTimeout to AppError.CatalogTimeout,
                HttpStatusCode.UnprocessableEntity to AppError.CatalogRequestRejected,
            )
            for ((code, expected) in cases) {
                status = code
                assertEquals(Outcome.Failure(expected), reader.search(""))
            }
        }
    }

    /** Неполный и синтаксически неверный JSON возвращают ошибку данных. */
    @Test
    fun rejectsInvalidJsonAndMissingFields() = runTest {
        var body = ""
        withReader({ json(body) }) { reader ->
            for (bad in listOf("{", "{}", """{"items":[{"title":"Столицы"}]}""")) {
                body = bad
                assertEquals(Outcome.Failure(AppError.InvalidCatalogData), reader.search(""))
            }
        }
    }

    /** Ответ с неподдерживаемым типом содержимого отклоняется. */
    @Test
    fun rejectsUnexpectedContentType() = runTest {
        withReader({ respond("<html>Service</html>") }) { reader ->
            assertEquals(Outcome.Failure(AppError.InvalidCatalogData), reader.search(""))
        }
    }

    /** HTTPS-клиент не обращается к хосту из редиректа. */
    @Test
    fun doesNotFollowRedirects() = runTest {
        var calls = 0
        withReader({
            calls++
            respond(
                "",
                HttpStatusCode.Found,
                headersOf(HttpHeaders.Location, "https://other.example"),
            )
        }) { reader ->
            assertEquals(Outcome.Failure(AppError.InvalidCatalogData), reader.search(""))
            assertEquals(1, calls)
        }
    }

    /** Ошибки DNS, соединения и чтения отличают сбой сети от таймаута. */
    @Test
    fun distinguishesNetworkFailureAndTransportTimeouts() = runTest {
        var failure: Exception = UnknownHostException("No DNS")
        withReader({ throw failure }) { reader ->
            assertEquals(Outcome.Failure(AppError.Network), reader.search(""))
            failure = ConnectTimeoutException("Slow connect")
            assertEquals(Outcome.Failure(AppError.CatalogTimeout), reader.search(""))
            failure = SocketTimeoutException("Slow read")
            assertEquals(Outcome.Failure(AppError.CatalogTimeout), reader.search(""))
        }
    }

    /** Зависший запрос ограничен 15 секундами виртуального времени. */
    @Test
    fun capsEntireCallAtFifteenSeconds() = runTest {
        withReader({ awaitCancellation() }) { reader ->
            val start = currentTime
            assertEquals(Outcome.Failure(AppError.CatalogTimeout), reader.search(""))
            assertEquals(15_000L, currentTime - start)
        }
    }

    /** Отмена вызывающей корутины сохраняется при незавершённом запросе. */
    @Test
    fun propagatesTimeoutFromCaller() = runTest {
        withReader({ awaitCancellation() }) { reader ->
            try {
                withTimeout(100L) { reader.search("") }
                error("Expected caller cancellation")
            } catch (_: TimeoutCancellationException) {
                assertEquals(100L, currentTime)
            }
        }
    }

    private suspend fun TestScope.withReader(
        handler: MockRequestHandler,
        check: suspend (RemoteCatalogReader) -> Unit,
    ) {
        val client = HttpClient(MockEngine) {
            configureCatalogHttp()
            engine {
                dispatcher = StandardTestDispatcher(testScheduler)
                addHandler(handler)
            }
        }
        try {
            check(RemoteCatalogReader(client, origin))
        } finally {
            client.close()
        }
    }

    private fun MockRequestHandleScope.json(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpResponseData =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json; charset=utf-8"))
}
