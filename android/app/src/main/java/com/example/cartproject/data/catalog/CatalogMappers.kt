package com.example.cartproject.data.catalog

import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.CatalogCard
import com.example.cartproject.domain.decks.CatalogCardId
import com.example.cartproject.domain.decks.CatalogDeckContent
import com.example.cartproject.domain.decks.CatalogDeckId
import com.example.cartproject.domain.decks.CatalogDeckSummary
import com.example.cartproject.domain.decks.MaterialValidator
import com.example.cartproject.domain.decks.validatedFor
import java.util.UUID

private val invalidCatalogData = Outcome.Failure(AppError.InvalidCatalogData)

/** Проверяет выдачу и сохраняет порядок, установленный каталогом. */
internal fun CatalogListDto.toDomain(): Outcome<List<CatalogDeckSummary>> {
    val seen = mutableSetOf<CatalogDeckId>()
    val summaries = items.map { dto ->
        val uuid = parseUuid(dto.id) ?: return invalidCatalogData
        val id = CatalogDeckId(uuid)
        if (!seen.add(id) || dto.cardCount < 0) return invalidCatalogData
        val text = (MaterialValidator.deck(dto.title, dto.description) as? Outcome.Success)?.value
            ?: return invalidCatalogData
        CatalogDeckSummary(id, text.title, text.description, dto.cardCount)
    }
    return Outcome.Success(summaries)
}

/** Преобразует полный ответ и проверяет, что он относится к запрошенному набору. */
internal fun CatalogDetailDto.toDomain(requestedId: CatalogDeckId): Outcome<CatalogDeckContent> {
    val uuid = parseUuid(id) ?: return invalidCatalogData
    val mapped = cards.map { dto ->
        val cardId = parseUuid(dto.id) ?: return invalidCatalogData
        CatalogCard(CatalogCardId(cardId), dto.question, dto.answer)
    }
    return CatalogDeckContent(CatalogDeckId(uuid), title, description, mapped)
        .validatedFor(requestedId)
}

private fun parseUuid(value: String): UUID? = try {
    UUID.fromString(value).takeIf { it.toString().equals(value, ignoreCase = true) }
} catch (_: IllegalArgumentException) {
    null
}
