package com.example.cartproject.data.local

import com.example.cartproject.domain.decks.Card
import com.example.cartproject.domain.decks.CatalogDeckId
import com.example.cartproject.domain.decks.Deck
import com.example.cartproject.domain.decks.DeckOrigin
import com.example.cartproject.domain.decks.LocalCardId
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.domain.study.ReviewSchedule
import com.example.cartproject.domain.study.ScheduledCard
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

/** Проверяет преобразование моделей Room без базы данных. */
class LocalMappingsTest {
    /** Сохраняет локальный ID и различает свои и скачанные наборы. */
    @Test
    fun mapsBothKindsOfDeck() {
        val owned = Deck(LocalDeckId(7), "Столицы", "Европа")
        val sourceId = UUID(8, 9)
        val row = DeckEntity(7, "Столицы", "Европа")
        assertEquals(owned, row.toDomain())
        assertEquals(
            owned.copy(origin = DeckOrigin.Downloaded(CatalogDeckId(sourceId))),
            row.copy(sourceId = sourceId.toString()).toDomain(),
        )
    }

    /** Сохраняет ревизию карточки, стадию и точный срок в миллисекундах. */
    @Test
    fun mapsCardAndScheduleAsOneRow() {
        val card = Card(LocalCardId(4), LocalDeckId(3), "Один\nДва", "Три", revision = 6)
        val at = Instant.parse("2026-11-25T11:00:00.123Z")
        val joined = ScheduledCardRow(card.toEntity(), 3, at.toEpochMilli(), 8)
        val schedule = ReviewSchedule(3, at, 8)
        assertEquals(ScheduledCard(card, schedule), joined.toDomain())
        assertEquals(ReviewScheduleEntity(4, 3, at.toEpochMilli(), 8), schedule.toEntity(4))
    }
}
