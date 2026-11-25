package com.example.cartproject.domain.decks

import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.flatMap
import com.example.cartproject.domain.study.Clock
import com.example.cartproject.domain.study.SystemClock

/** Проверяет поисковый запрос и использует правила поиска публичного каталога. */
class SearchCatalog(private val reader: CatalogReader) {
    /** Удаляет внешние пробелы и передаёт запрос в каталог. */
    suspend operator fun invoke(query: String): Outcome<List<CatalogDeckSummary>> =
        MaterialValidator.search(query).flatMap { reader.search(it) }
}

/** Выбирает источник в зависимости от типа идентификатора. */
class ReadDeck(private val local: LocalDeckStore, private val catalog: CatalogReader) {
    /** Читает собственный набор или снимок на устройстве. */
    suspend operator fun invoke(id: LocalDeckId): Outcome<LocalDeckContent> = local.readDeck(id)

    /** Читает свежую версию набора из каталога. */
    suspend operator fun invoke(id: CatalogDeckId): Outcome<CatalogDeckContent> =
        catalog.readDeck(id)
}

/** Проверяет серверный набор и передаёт его для атомарного сохранения. */
class DownloadDeck(
    private val catalog: CatalogReader,
    private val local: LocalDeckStore,
    private val clock: Clock = SystemClock,
) {
    /** Возвращает существующий снимок или сохраняет набор со всеми карточками. */
    suspend operator fun invoke(id: CatalogDeckId): Outcome<LocalDeckId> =
        local.findDownloadedDeck(id).flatMap { existing ->
            if (existing != null) {
                Outcome.Success(existing)
            } else {
                catalog.readDeck(id)
                    .flatMap { it.validatedFor(id) }
                    .flatMap { local.saveDownloadedDeck(it, clock.now()) }
            }
        }
}

/** Проверяет ID снимка, уникальность карточек и текст перед показом или сохранением. */
fun CatalogDeckContent.validatedFor(requestedId: CatalogDeckId): Outcome<CatalogDeckContent> {
    val invalid = Outcome.Failure(AppError.InvalidCatalogData)
    if (id != requestedId) return invalid
    val text =
        (MaterialValidator.deck(title, description) as? Outcome.Success)?.value ?: return invalid
    val ids = mutableSetOf<CatalogCardId>()
    val normalizedCards = cards.map { card ->
        if (!ids.add(card.id)) return invalid
        val fields = (MaterialValidator.card(card.question, card.answer) as? Outcome.Success)?.value
            ?: return invalid
        card.copy(question = fields.question, answer = fields.answer)
    }
    return Outcome.Success(
        copy(title = text.title, description = text.description, cards = normalizedCards),
    )
}
