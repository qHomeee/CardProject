package com.example.cartproject.domain.decks

import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.study.ReviewSchedule
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Проверяет команды редактирования и поток локальных наборов. */
class LocalMaterialUseCasesTest {
    private val local = LocalDeckStoreFake()
    private val now = Instant.parse("2026-11-24T10:00:00Z")
    private val saveDeck = SaveOwnDeck(local)
    private val saveCard = SaveOwnCard(local) { now }
    private val delete = DeleteOwnMaterial(local)
    private val owned = Deck(LocalDeckId(1), "География", "")
    private val downloaded = Deck(
        LocalDeckId(2),
        "Столицы",
        "",
        DeckOrigin.Downloaded(CatalogDeckId(UUID(3, 4))),
    )

    /** Поток передаёт пустой список и обновление после добавления набора. */
    @Test
    fun observesLocalDecksAsTheyChange() = runTest {
        val observed = async(start = CoroutineStart.UNDISPATCHED) {
            ObserveMyDecks(local)().take(2).toList()
        }
        local.add(owned)
        assertEquals(
            listOf(Outcome.Success(emptyList<Deck>()), Outcome.Success(listOf(owned))),
            observed.await(),
        )
    }

    /** Одинаковые названия допустимы, пустой набор сохраняется. */
    @Test
    fun createsEmptyDecksWithDuplicateTitles() = runTest {
        val first = (saveDeck(null, " История ", "") as Outcome.Success).value
        val second = (saveDeck(null, "История", "") as Outcome.Success).value
        assertTrue(first != second)
        val expected = LocalDeckContent(Deck(first, "История", ""), emptyList())
        assertEquals(Outcome.Success(expected), local.readDeck(first))
    }

    /** Некорректные поля останавливают команду до обращения к хранилищу. */
    @Test
    fun validatesBeforeWritingOwnMaterial() = runTest {
        assertTrue(saveDeck(null, "  ", "") is Outcome.Failure)
        assertTrue(saveCard(owned.id, null, "Вопрос", "") is Outcome.Failure)
        assertEquals(0, local.ownWrites)
    }

    /** Правка текста меняет расписание, новое название набора его сохраняет. */
    @Test
    fun cardEditResetsScheduleAndDeckRenamePreservesIt() = runTest {
        local.add(owned)
        val id = (
            saveCard(
                owned.id,
                null,
                "Столица Португалии",
                "Лиссабон",
            ) as Outcome.Success
            ).value
        val initial = local.cardRows.getValue(id)
        assertEquals(ReviewSchedule.initial(now), initial.schedule)
        val progressed = initial.copy(schedule = ReviewSchedule(3, now.plusSeconds(600), 4))
        local.cardRows[id] = progressed
        assertEquals(Outcome.Success(owned.id), saveDeck(owned.id, " Мои страны ", ""))
        assertEquals(progressed, local.cardRows.getValue(id))
        assertEquals(
            Outcome.Success(id),
            saveCard(owned.id, id, "Португалия — столица?", " Лиссабон "),
        )
        val changed = local.cardRows.getValue(id)
        assertEquals("Португалия — столица?", changed.card.question)
        assertEquals(1L, changed.card.revision)
        assertEquals(ReviewSchedule(0, now, 5), changed.schedule)
    }

    /** Снимок защищён от редактирования и удаляется отдельной командой. */
    @Test
    fun downloadedDeckUsesSeparateDeleteCommand() = runTest {
        local.add(downloaded)
        val denied = Outcome.Failure(AppError.ReadOnlyDeck)
        assertEquals(denied, saveDeck(downloaded.id, "Новое", ""))
        assertEquals(denied, saveCard(downloaded.id, null, "Вопрос", "Ответ"))
        assertEquals(denied, delete.deck(downloaded.id))
        assertEquals(Outcome.Success(Unit), RemoveDownloadedDeck(local)(downloaded.id))
        assertEquals(Outcome.Failure(AppError.DeckNotFound), local.readDeck(downloaded.id))
    }

    /** Удаление карточки ограничено своим набором, удаление набора убирает оставшиеся карточки. */
    @Test
    fun deletesCardsWithinTheirDeck() = runTest {
        val other = owned.copy(id = LocalDeckId(9))
        local.add(owned)
        local.add(other)
        val id = (saveCard(owned.id, null, "Вопрос 1", "Ответ") as Outcome.Success).value
        val another = (saveCard(owned.id, null, "Вопрос 2", "Ответ") as Outcome.Success).value
        assertEquals(Outcome.Failure(AppError.CardNotFound), delete.card(other.id, id))
        assertEquals(Outcome.Success(Unit), delete.card(owned.id, id))
        assertEquals(setOf(another), local.cardRows.keys)
        assertEquals(Outcome.Success(Unit), delete.deck(owned.id))
        assertTrue(local.cardRows.isEmpty())
    }
}
