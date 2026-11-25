package com.example.cartproject.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Читает и записывает строки локального материала. */
@Dao
internal interface DeckDao {
    /** Наблюдает список в порядке локальных ID. */
    @Query("SELECT * FROM decks ORDER BY id")
    fun observeDecks(): Flow<List<DeckEntity>>

    /** Находит набор по локальному ID. */
    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun deck(id: Long): DeckEntity?

    /** Находит уже скачанную копию. */
    @Query("SELECT id FROM decks WHERE source_id = :sourceId")
    suspend fun downloadedDeckId(sourceId: String): Long?

    /** Читает карточки в стабильном порядке. */
    @Query("SELECT * FROM cards WHERE deck_id = :deckId ORDER BY id")
    suspend fun cards(deckId: Long): List<CardEntity>

    /** Создаёт набор и возвращает его ID. */
    @Insert
    suspend fun insertDeck(entity: DeckEntity): Long

    /** Обновляет набор с сохранением его ID. */
    @Update
    suspend fun updateDeck(entity: DeckEntity)

    /** Создаёт карточку и возвращает её ID. */
    @Insert
    suspend fun insertCard(entity: CardEntity): Long

    /** Создаёт строки карточек снимка. */
    @Insert
    suspend fun insertCards(entities: List<CardEntity>): List<Long>

    /** Записывает изменившийся текст и его ревизию. */
    @Update
    suspend fun updateCard(entity: CardEntity)

    /** Удаляет набор и все связанные строки. */
    @Query("DELETE FROM decks WHERE id = :id")
    suspend fun deleteDeck(id: Long)

    /** Удаляет карточку только из указанного набора. */
    @Query("DELETE FROM cards WHERE deck_id = :deckId AND id = :cardId")
    suspend fun deleteCard(deckId: Long, cardId: Long): Int
}
