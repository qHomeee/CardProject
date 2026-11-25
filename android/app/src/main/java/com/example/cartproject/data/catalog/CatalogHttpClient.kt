package com.example.cartproject.data.catalog

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal const val CATALOG_TIMEOUT_MILLIS = 15_000L

/** Создаёт клиент публичного каталога с системной проверкой TLS. */
internal fun createCatalogHttpClient(): HttpClient = HttpClient(OkHttp) {
    configureCatalogHttp()
    engine {
        config { retryOnConnectionFailure(false) }
    }
}

/** Применяет общую конфигурацию к рабочему клиенту и MockEngine. */
internal fun HttpClientConfig<*>.configureCatalogHttp() {
    expectSuccess = false
    followRedirects = false
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = CATALOG_TIMEOUT_MILLIS
        connectTimeoutMillis = 5_000L
        socketTimeoutMillis = 10_000L
    }
}
