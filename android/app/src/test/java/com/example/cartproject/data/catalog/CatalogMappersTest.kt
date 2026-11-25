package com.example.cartproject.data.catalog

import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.CatalogCard
import com.example.cartproject.domain.decks.CatalogCardId
import com.example.cartproject.domain.decks.CatalogDeckContent
import com.example.cartproject.domain.decks.CatalogDeckId
import com.example.cartproject.domain.decks.CatalogDeckSummary
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

/** Проверяет преобразование сетевых моделей до передачи в домен. */
class CatalogMappersTest {
    private val uuid = "356f61bb-a918-4e0f-8cd8-369c1f0e318a"
    private val id = CatalogDeckId(UUID.fromString(uuid))
    private val cardUuid = "184c9b9e-7ca9-4440-ac60-aa682141ee33"
    private val summary = CatalogSummaryDto(uuid, " Столицы ", " Европа ", 1)
    private val card = CatalogCardDto(cardUuid, " Какой город?\nПодсказка ", " Лиссабон ")
    private val detail = CatalogDetailDto(uuid, " Столицы ", " Европа ", listOf(card))
    private val invalid = Outcome.Failure(AppError.InvalidCatalogData)

    /** Выдача сохраняет порядок и разные ID у одноимённых наборов. */
    @Test
    fun preservesOrderAndDistinctIds() {
        val another = UUID(0, 1)
        val result = CatalogListDto(
            listOf(summary.copy(id = another.toString()), summary),
        ).toDomain()
        val expected = listOf(
            CatalogDeckSummary(CatalogDeckId(another), "Столицы", "Европа", 1),
            CatalogDeckSummary(id, "Столицы", "Европа", 1),
        )
        assertEquals(Outcome.Success(expected), result)
    }

    /** Повтор UUID и отрицательное количество карточек считаются ошибками данных. */
    @Test
    fun rejectsDuplicateIdsAndNegativeCount() {
        assertEquals(
            invalid,
            CatalogListDto(listOf(summary, summary.copy(id = uuid.uppercase()))).toDomain(),
        )
        assertEquals(invalid, CatalogListDto(listOf(summary.copy(cardCount = -1))).toDomain())
    }

    /** Сокращённый UUID отвергается, даже если Java может разобрать строку. */
    @Test
    fun rejectsShortUuidAndInvalidTitle() {
        assertEquals(invalid, CatalogListDto(listOf(summary.copy(id = "1-2-3-4-5"))).toDomain())
        assertEquals(invalid, CatalogListDto(listOf(summary.copy(title = " "))).toDomain())
    }

    /** Полный ответ сохраняет внутренний перенос и нормализует края. */
    @Test
    fun mapsFullSnapshotWithNormalizedText() {
        val expectedCard = CatalogCard(
            CatalogCardId(UUID.fromString(cardUuid)),
            "Какой город?\nПодсказка",
            "Лиссабон",
        )
        val expected = CatalogDeckContent(id, "Столицы", "Европа", listOf(expectedCard))
        assertEquals(Outcome.Success(expected), detail.toDomain(id))
    }

    /** Чужой набор и некорректные карточки отклоняются целиком. */
    @Test
    fun rejectsMismatchedDeckAndInvalidCards() {
        assertEquals(invalid, detail.toDomain(CatalogDeckId(UUID(0, 2))))
        assertEquals(invalid, detail.copy(cards = listOf(card, card)).toDomain(id))
        assertEquals(invalid, detail.copy(cards = listOf(card.copy(id = "broken"))).toDomain(id))
        assertEquals(invalid, detail.copy(cards = listOf(card.copy(answer = " "))).toDomain(id))
    }
}
