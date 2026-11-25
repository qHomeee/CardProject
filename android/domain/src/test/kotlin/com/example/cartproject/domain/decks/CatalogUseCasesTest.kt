package com.example.cartproject.domain.decks

import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.study.ReviewSchedule
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Проверяет работу с каталогом без сети. */
class CatalogUseCasesTest {
    private val source = CatalogDeckId(UUID(1, 1))
    private val otherSource = CatalogDeckId(UUID(1, 2))
    private val now = Instant.parse("2026-11-24T10:00:00Z")
    private val card = CatalogCard(CatalogCardId(UUID(2, 1)), " Столица\nПортугалии ", " Лиссабон ")
    private val content =
        CatalogDeckContent(source, " Столицы Европы ", " География ", listOf(card))
    private val catalog = CatalogReaderFake()
    private val local = LocalDeckStoreFake()
    private val download = DownloadDeck(catalog, local) { now }

    /** Поиск нормализует пробелы и сохраняет результаты с разными ID. */
    @Test
    fun normalizesSearchAndPreservesDistinctIds() = runTest {
        val first = CatalogDeckSummary(source, "Столицы Европы", "", 1)
        val second = first.copy(id = otherSource)
        val found = Outcome.Success(listOf(first, second))
        catalog.rows = listOf(first, second)
        val search = SearchCatalog(catalog)
        assertEquals(found, search("  еВРОП\n"))
        assertEquals(found, search(" \t"))
        assertEquals(Outcome.Success(emptyList<CatalogDeckSummary>()), search("океан"))
        assertEquals(listOf("еВРОП", "", "океан"), catalog.queries)
    }

    /** Неверный запрос останавливается до транспорта, сетевой сбой передаётся вызывающей стороне. */
    @Test
    fun rejectsLongSearchAndPropagatesNetworkError() = runTest {
        val search = SearchCatalog(catalog)
        val invalid = AppError.InvalidFields(mapOf(MaterialField.Search to MaterialIssue.TooLong))
        assertEquals(Outcome.Failure(invalid), search("а".repeat(101)))
        assertTrue(catalog.queries.isEmpty())
        catalog.error = AppError.Network
        assertEquals(Outcome.Failure(AppError.Network), search("Москва"))
    }

    /** Снимок открывается локально даже после удаления каталожного оригинала. */
    @Test
    fun selectsReadSourceByIdType() = runTest {
        val deck = Deck(LocalDeckId(1), "Копия", "", DeckOrigin.Downloaded(source))
        local.add(deck)
        val read = ReadDeck(local, catalog)
        val expected = Outcome.Success(LocalDeckContent(deck, emptyList()))
        assertEquals(expected, read(deck.id))
        assertEquals(0, catalog.reads)
        catalog.content = content
        assertEquals(Outcome.Success(content), read(source))
        catalog.content = null
        assertEquals(Outcome.Failure(AppError.DeckNotFound), read(source))
        assertEquals(expected, read(deck.id))
    }

    /** Снимок сохраняет все карточки с расписанием и переиспользуется офлайн. */
    @Test
    fun downloadsSnapshotAndReusesItOffline() = runTest {
        catalog.content =
            content.copy(cards = listOf(card, card.copy(id = CatalogCardId(UUID(2, 2)))))
        val first = download(source) as Outcome.Success
        val saved = (local.readDeck(first.value) as Outcome.Success).value
        assertEquals(DeckOrigin.Downloaded(source), saved.deck.origin)
        assertEquals("Столицы Европы", saved.deck.title)
        assertEquals("География", saved.deck.description)
        assertEquals(List(2) { "Столица\nПортугалии" }, saved.cards.map { it.question })
        assertEquals(
            setOf(ReviewSchedule.initial(now)),
            local.cardRows.values.map {
                it.schedule
            }.toSet(),
        )
        catalog.error = AppError.Network
        assertEquals(first, download(source))
        assertEquals(1, catalog.reads)
        assertEquals(1, local.downloadWrites)
    }

    /** Неверный ID, повтор карточки и пустой ответ отклоняются до записи. */
    @Test
    fun rejectsInvalidSnapshotsBeforeStorage() = runTest {
        val invalid = Outcome.Failure(AppError.InvalidCatalogData)
        for (bad in listOf(
            content.copy(id = otherSource),
            content.copy(cards = listOf(card, card)),
            content.copy(cards = listOf(card.copy(answer = " \n"))),
        )) {
            catalog.content = bad
            assertEquals(invalid, download(source))
        }
        assertEquals(0, local.downloadWrites)
        assertEquals(Outcome.Success(emptyList<Deck>()), local.observeDecks().first())
    }

    /** Сбои загрузки и хранилища оставляют возможность повторить команду. */
    @Test
    fun allowsRetryAfterDownloadFailure() = runTest {
        catalog.error = AppError.Network
        assertEquals(Outcome.Failure(AppError.Network), download(source))
        assertEquals(0, local.downloadWrites)
        catalog.error = null
        catalog.content = content
        local.failDownload = true
        assertEquals(Outcome.Failure(AppError.LocalWrite), download(source))
        assertEquals(Outcome.Success(emptyList<Deck>()), local.observeDecks().first())
        assertTrue(local.cardRows.isEmpty())
        local.failDownload = false
        assertTrue(download(source) is Outcome.Success)
        assertEquals(1, (local.observeDecks().first() as Outcome.Success).value.size)
    }

    /** Одновременное скачивание сохраняет появившийся снимок. */
    @Test
    fun returnsSnapshotCreatedByConcurrentDownload() = runTest {
        catalog.content = content
        val other = Deck(LocalDeckId(5), "Существующий снимок", "", DeckOrigin.Downloaded(source))
        local.beforeDownloadWrite = { local.add(other) }
        assertEquals(Outcome.Success(other.id), download(source))
        assertEquals(Outcome.Success(listOf(other)), local.observeDecks().first())
        assertTrue(local.cardRows.isEmpty())
    }
}

/** Подменяет публичный каталог в сценарных тестах. */
private class CatalogReaderFake : CatalogReader {
    var rows: List<CatalogDeckSummary> = emptyList()
    var content: CatalogDeckContent? = null
    var error: AppError? = null
    val queries = mutableListOf<String>()
    var reads = 0
        private set

    override suspend fun search(query: String): Outcome<List<CatalogDeckSummary>> {
        queries += query
        return error?.let { Outcome.Failure(it) }
            ?: Outcome.Success(rows.filter { it.title.contains(query, ignoreCase = true) })
    }

    override suspend fun readDeck(id: CatalogDeckId): Outcome<CatalogDeckContent> {
        reads++
        return error?.let { Outcome.Failure(it) }
            ?: content?.let { Outcome.Success(it) }
            ?: Outcome.Failure(AppError.DeckNotFound)
    }
}
