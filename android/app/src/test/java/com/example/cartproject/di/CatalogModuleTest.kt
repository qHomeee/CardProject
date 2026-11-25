package com.example.cartproject.di

import com.example.cartproject.data.catalog.RemoteCatalogReader
import com.example.cartproject.domain.decks.CatalogReader
import com.example.cartproject.domain.decks.SearchCatalog
import io.ktor.client.HttpClient
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.dsl.koinApplication

/** Проверяет подключение каталога в графе приложения. */
class CatalogModuleTest {
    /** Один клиент используется контейнером и закрывается при его остановке. */
    @Test
    fun resolvesCatalogFromAppGraphAndClosesHttpClient() = runTest {
        val app = koinApplication { modules(appModule) }
        val client = try {
            val reader = app.koin.get<CatalogReader>()
            assertTrue(reader is RemoteCatalogReader)
            assertSame(reader, app.koin.get<CatalogReader>())
            assertNotNull(app.koin.get<SearchCatalog>())
            val http = app.koin.get<HttpClient>()
            assertSame(http, app.koin.get<HttpClient>())
            http
        } finally {
            app.close()
        }
        client.coroutineContext.job.join()
        assertTrue(client.coroutineContext.job.isCompleted)
    }
}
