package com.example.cartproject.domain.decks

import com.example.cartproject.domain.Outcome

/** Набор в списке общего каталога. */
data class CatalogDeckSummary(
    val id: CatalogDeckId,
    val title: String,
    val description: String,
    val cardCount: Int,
)

/** Карточка общего каталога с идентификатором сервера. */
data class CatalogCard(val id: CatalogCardId, val question: String, val answer: String)

/** Полный снимок серверного набора для просмотра или скачивания. */
data class CatalogDeckContent(
    val id: CatalogDeckId,
    val title: String,
    val description: String,
    val cards: List<CatalogCard>,
)

/** Публичное чтение каталога независимо от транспорта. */
interface CatalogReader {
    /** Ищет подстроку названия без учёта регистра. Пустой запрос возвращает каталог. */
    suspend fun search(query: String): Outcome<List<CatalogDeckSummary>>

    /** Получает набор и все карточки из публичного каталога. */
    suspend fun readDeck(id: CatalogDeckId): Outcome<CatalogDeckContent>
}
