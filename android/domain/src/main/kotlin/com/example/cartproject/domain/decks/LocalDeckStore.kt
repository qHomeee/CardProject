package com.example.cartproject.domain.decks

import com.example.cartproject.domain.Outcome
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** Чтение списка локальных наборов для экрана «Мои наборы». */
fun interface LocalDeckList {
    /** Наблюдает изменения списка и сообщает об ошибке чтения. */
    fun observeDecks(): Flow<Outcome<List<Deck>>>
}

/** Данные для создания или редактирования собственной карточки. */
data class OwnCardWrite(
    val deckId: LocalDeckId,
    val cardId: LocalCardId?,
    val text: CardText,
    val editedAt: Instant,
)

/** Хранилище локальных наборов, карточек и снимков каталога. */
interface LocalDeckStore : LocalDeckList {
    /** Возвращает набор и его карточки одним снимком. */
    suspend fun readDeck(id: LocalDeckId): Outcome<LocalDeckContent>

    /** Записывает свой набор. При обновлении проверяет происхождение в той же операции. */
    suspend fun saveOwnDeck(id: LocalDeckId?, text: DeckText): Outcome<LocalDeckId>

    /**
     * Проверяет происхождение набора и принадлежность карточки. В одной транзакции
     * создаёт начальное расписание или применяет ScheduledCard.revise к имеющейся записи.
     */
    suspend fun saveOwnCard(write: OwnCardWrite): Outcome<LocalCardId>

    /** Возвращает ID локального снимка, если набор уже скачан. */
    suspend fun findDownloadedDeck(sourceId: CatalogDeckId): Outcome<LocalDeckId?>

    /**
     * Сохраняет набор, все карточки и их расписания вместе. Повторная или одновременная
     * запись по тому же ID источника возвращает существующий снимок с его прогрессом.
     */
    suspend fun saveDownloadedDeck(
        content: CatalogDeckContent,
        receivedAt: Instant,
    ): Outcome<LocalDeckId>

    /** Удаляет собственный набор с карточками, расписанием и историей. */
    suspend fun deleteOwnDeck(id: LocalDeckId): Outcome<Unit>

    /** Удаляет карточку собственного набора вместе с её расписанием и историей. */
    suspend fun deleteOwnCard(deckId: LocalDeckId, cardId: LocalCardId): Outcome<Unit>

    /** Удаляет локальный снимок каталога и его историю. */
    suspend fun removeDownloadedDeck(id: LocalDeckId): Outcome<Unit>
}
