package com.example.cartproject.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.CatalogCard
import com.example.cartproject.domain.decks.CatalogCardId
import com.example.cartproject.domain.decks.CatalogDeckContent
import com.example.cartproject.domain.decks.CatalogDeckId
import com.example.cartproject.domain.decks.Deck
import com.example.cartproject.domain.decks.DeckOrigin
import com.example.cartproject.domain.decks.DeleteOwnMaterial
import com.example.cartproject.domain.decks.RemoveDownloadedDeck
import com.example.cartproject.domain.decks.SaveOwnCard
import com.example.cartproject.domain.decks.SaveOwnDeck
import com.example.cartproject.domain.study.ReviewSchedule
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Проверяет транзакции локальных наборов в настоящей SQLite на Android. */
@RunWith(AndroidJUnit4::class)
class RoomDeckStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, ZapomniDatabase::class.java).build()
    private val store = RoomLocalDeckStore(db)
    private val now = Instant.parse("2026-11-25T11:00:00Z")
    private val sourceId = CatalogDeckId(UUID(0, 9))
    private val snapshot = CatalogDeckContent(
        sourceId,
        "Столицы",
        "Европа",
        listOf(
            CatalogCard(CatalogCardId(UUID(0, 1)), "Португалия", "Лиссабон"),
            CatalogCard(CatalogCardId(UUID(0, 2)), "Испания", "Мадрид"),
        ),
    )

    /** Освобождает тестовую базу после каждого сценария. */
    @After
    fun tearDown() {
        db.close()
    }

    /** Переименование набора оставляет срок, правка текста изменяет версию карточки. */
    @Test
    fun editsOwnCardAndKeepsOtherData() = runBlocking {
        val saveDeck = SaveOwnDeck(store)
        var time = now
        val saveCard = SaveOwnCard(store) { time }
        val id = (saveDeck(null, " Столицы ", "") as Outcome.Success).value
        val other = (saveDeck(null, "Столицы", "") as Outcome.Success).value
        val card = (saveCard(id, null, "Португалия", "Лиссабон") as Outcome.Success).value
        val initial = db.reviews().scheduledCard(id.value, card.value)!!.toDomain()
        assertEquals(ReviewSchedule.initial(now), initial.schedule)
        assertEquals(Outcome.Success(id), saveDeck(id, "Европа", ""))
        assertEquals(initial, db.reviews().scheduledCard(id.value, card.value)!!.toDomain())
        val wrongDeck = DeleteOwnMaterial(store).card(other, card)
        assertEquals(Outcome.Failure(AppError.CardNotFound), wrongDeck)
        time = now.plusSeconds(60)
        assertEquals(Outcome.Success(card), saveCard(id, card, "Столица Португалии", "Лиссабон"))
        val edited = db.reviews().scheduledCard(id.value, card.value)!!.toDomain()
        assertEquals("Столица Португалии", edited.card.question)
        assertEquals(1L, edited.card.revision)
        assertEquals(ReviewSchedule(0, time, 1), edited.schedule)
    }

    /** Два одновременных сохранения возвращают один ID и один комплект карточек. */
    @Test
    fun concurrentDownloadsKeepOneSnapshot() = runBlocking {
        val first = async { store.saveDownloadedDeck(snapshot, now) }
        val second = async { store.saveDownloadedDeck(snapshot, now.plusSeconds(2)) }
        val result = first.await() as Outcome.Success
        assertEquals(result, second.await())
        assertEquals(1, (store.observeDecks().first() as Outcome.Success).value.size)
        val content = (store.readDeck(result.value) as Outcome.Success).value
        assertEquals(DeckOrigin.Downloaded(sourceId), content.deck.origin)
        assertEquals(2, content.cards.size)
        assertEquals(
            Outcome.Failure(AppError.ReadOnlyDeck),
            SaveOwnDeck(store)(result.value, "Другое", ""),
        )
        assertEquals(Outcome.Success(Unit), RemoveDownloadedDeck(store)(result.value))
        assertEquals(Outcome.Success(emptyList<Deck>()), store.observeDecks().first())
        assertEquals(0, rowCount("cards"))
        assertEquals(0, rowCount("review_schedules"))
    }

    /** Ошибка на второй карточке откатывает весь снимок. */
    @Test
    fun failedInsertRollsBackWholeDownload() = runBlocking {
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_second_card BEFORE INSERT ON cards
            WHEN NEW.question = 'Испания' BEGIN SELECT RAISE(ABORT, 'test write failure'); END
            """.trimIndent(),
        )
        assertEquals(Outcome.Failure(AppError.LocalWrite), store.saveDownloadedDeck(snapshot, now))
        assertEquals(Outcome.Success(emptyList<Deck>()), store.observeDecks().first())
        assertEquals(0, rowCount("cards"))
        assertEquals(0, rowCount("review_schedules"))
    }

    /** Ошибка чтения Room возвращается через поток. */
    @Test
    fun observesReadErrorAfterDatabaseCloses() = runBlocking {
        assertEquals(Outcome.Success(emptyList<Deck>()), store.observeDecks().first())
        db.close()
        val outcome = withTimeout(3_000L) { store.observeDecks().first() }
        assertEquals(Outcome.Failure(AppError.LocalRead), outcome)
    }

    private fun rowCount(table: String): Int = db.openHelper.readableDatabase
        .query("SELECT COUNT(*) FROM $table").use { cursor ->
            check(cursor.moveToFirst())
            cursor.getInt(0)
        }
}
