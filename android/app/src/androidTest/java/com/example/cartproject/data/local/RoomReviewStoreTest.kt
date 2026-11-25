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
import com.example.cartproject.domain.decks.CatalogDeckSummary
import com.example.cartproject.domain.decks.CatalogReader
import com.example.cartproject.domain.decks.DeleteOwnMaterial
import com.example.cartproject.domain.decks.DownloadDeck
import com.example.cartproject.domain.decks.LocalCardId
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.domain.decks.ReadDeck
import com.example.cartproject.domain.decks.SaveOwnCard
import com.example.cartproject.domain.decks.SaveOwnDeck
import com.example.cartproject.domain.study.GradeCard
import com.example.cartproject.domain.study.ReadStatistics
import com.example.cartproject.domain.study.ReviewGrade
import com.example.cartproject.domain.study.ReviewKey
import com.example.cartproject.domain.study.ReviewSchedule
import com.example.cartproject.domain.study.StartStudy
import com.example.cartproject.domain.study.StudySessionId
import com.example.cartproject.domain.study.StudyStart
import com.example.cartproject.domain.study.StudySummary
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Проверяет историю, откат и повторное открытие файловой базы. */
@RunWith(AndroidJUnit4::class)
class RoomReviewStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = Instant.parse("2026-11-25T11:00:00.123Z")
    private val sessionId = StudySessionId(UUID(0, 7))

    /** Повторные команды сохраняют одну запись, удаление карточки убирает связанные строки. */
    @Test
    fun replayKeepsOriginalAndDeleteCascades() = runBlocking {
        Room.inMemoryDatabaseBuilder(context, ZapomniDatabase::class.java).build().useDb { db ->
            val decks = RoomLocalDeckStore(db)
            val reviews = RoomReviewStore(db)
            val (deck, card) = createOwnCard(decks)
            val session = start(reviews, deck)
            val grade = GradeCard(reviews) { now }
            val first = async { grade(session, card, ReviewGrade.Remembered) }
            val second = async { grade(session, card, ReviewGrade.Remembered) }
            val saved = first.await() as Outcome.Success
            assertEquals(saved, second.await())
            assertEquals(now.plusSeconds(86_400), saved.value.schedule.dueAt)
            assertEquals(
                Outcome.Failure(AppError.ReviewConflict),
                grade(session, card, ReviewGrade.NeedsReview),
            )
            assertEquals(Outcome.Success(StudySummary(1, 0)), ReadStatistics(reviews)(sessionId))
            assertEquals(Outcome.Success(Unit), DeleteOwnMaterial(decks).card(deck, card))
            assertEquals(Outcome.Success(null), reviews.findReview(ReviewKey(sessionId, card)))
            assertEquals(Outcome.Success(emptyList<ReviewGrade>()), reviews.grades())
            assertEquals(
                Outcome.Failure(AppError.CardNotFound),
                reviews.readScheduledCard(deck, card),
            )
        }
    }

    /** Сбой вставки оценки откатывает срок, а изменение ревизии делает очередь устаревшей. */
    @Test
    fun rollsBackScheduleOnInsertErrorAndRejectsStaleRevision() = runBlocking {
        Room.inMemoryDatabaseBuilder(context, ZapomniDatabase::class.java).build().useDb { db ->
            val decks = RoomLocalDeckStore(db)
            val reviews = RoomReviewStore(db)
            val (deck, card) = createOwnCard(decks)
            val session = start(reviews, deck)
            val grade = GradeCard(reviews) { now }
            db.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER fail_review BEFORE INSERT ON reviews
                BEGIN SELECT RAISE(ABORT, 'test review write failure'); END
                """.trimIndent(),
            )
            val failedWrite = grade(session, card, ReviewGrade.NeedsReview)
            assertEquals(Outcome.Failure(AppError.LocalWrite), failedWrite)
            val persisted = (reviews.readScheduledCard(deck, card) as Outcome.Success).value
            assertEquals(ReviewSchedule.initial(now), persisted.schedule)
            assertEquals(Outcome.Success(null), reviews.findReview(ReviewKey(sessionId, card)))
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_review")
            val changed = SaveOwnCard(decks) { now }(deck, card, "Новый вопрос", "Лиссабон")
            assertEquals(Outcome.Success(card), changed)
            val stale = grade(session, card, ReviewGrade.Remembered)
            assertEquals(Outcome.Failure(AppError.ReviewConflict), stale)
        }
    }

    /** Скачанный набор, оценка и срок читаются из файловой базы после закрытия. */
    @Test
    fun downloadedDeckAndReviewSurviveReopeningWithoutNetwork() = runBlocking {
        val name = "offline-test-${UUID.randomUUID()}.db"
        val catalog = TestCatalog()
        try {
            val deckId = openZapomniDatabase(context, name).useDb { db ->
                val store = RoomLocalDeckStore(db)
                val reviews = RoomReviewStore(db)
                val downloaded = (
                    DownloadDeck(catalog, store) {
                        now
                    }(catalog.deck.id) as Outcome.Success
                    ).value
                val session = start(reviews, downloaded)
                val grade =
                    GradeCard(reviews) {
                        now
                    }(session, session.cards.first().id, ReviewGrade.Remembered)
                assertTrue(grade is Outcome.Success)
                downloaded
            }
            catalog.offline = true
            openZapomniDatabase(context, name).useDb { db ->
                val store = RoomLocalDeckStore(db)
                val reviews = RoomReviewStore(db)
                val contents = (ReadDeck(store, catalog)(deckId) as Outcome.Success).value
                assertEquals("Лиссабон", contents.cards.single().answer)
                assertEquals(
                    Outcome.Success(deckId),
                    DownloadDeck(catalog, store) {
                        now
                    }(catalog.deck.id),
                )
                assertEquals(1, catalog.reads)
                val next = StartStudy(reviews) { now }(deckId, StudySessionId(UUID(0, 8)))
                assertEquals(Outcome.Success(StudyStart.Empty(now.plusSeconds(86_400))), next)
                assertEquals(
                    Outcome.Success(StudySummary(1, 0)),
                    ReadStatistics(reviews)(sessionId),
                )
            }
        } finally {
            context.deleteDatabase(name)
        }
        Unit
    }

    private suspend fun createOwnCard(store: RoomLocalDeckStore): Pair<LocalDeckId, LocalCardId> {
        val deckId = (SaveOwnDeck(store)(null, "География", "") as Outcome.Success).value
        val result = SaveOwnCard(store) { now }(deckId, null, "Португалия", "Лиссабон")
        return deckId to (result as Outcome.Success).value
    }

    private suspend fun start(store: RoomReviewStore, deckId: LocalDeckId) = (
        (
            StartStudy(store) {
                now
            }(deckId, sessionId) as Outcome.Success
            ).value as StudyStart.Ready
        ).session

    private suspend fun <T> ZapomniDatabase.useDb(action: suspend (ZapomniDatabase) -> T): T = try {
        action(this)
    } finally {
        close()
    }

    /** Имитирует каталог, который становится недоступен после загрузки. */
    private class TestCatalog : CatalogReader {
        var reads = 0
        var offline = false
        val deck = CatalogDeckContent(
            CatalogDeckId(UUID(1, 1)),
            "Столицы",
            "Европа",
            listOf(CatalogCard(CatalogCardId(UUID(1, 2)), "Португалия", "Лиссабон")),
        )

        override suspend fun search(query: String): Outcome<List<CatalogDeckSummary>> =
            error("Only full deck reads are expected")

        override suspend fun readDeck(id: CatalogDeckId): Outcome<CatalogDeckContent> {
            reads++
            return if (offline) Outcome.Failure(AppError.Network) else Outcome.Success(deck)
        }
    }
}
