package com.example.cartproject.domain.decks

import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import org.junit.Assert.assertEquals
import org.junit.Test

/** Проверяет нормализацию и ограничения текстовых полей. */
class MaterialValidatorTest {
    /** Внешние пробелы отбрасываются, а переносы и пробелы внутри остаются. */
    @Test
    fun trimsOnlyOuterWhitespace() {
        val result = MaterialValidator.card("\u0085 \t Первая  строка\nВторая\u00A0", " Ответ\n")
        assertEquals(Outcome.Success(CardText("Первая  строка\nВторая", "Ответ")), result)
    }

    /** Дополнительные символы Unicode считаются по одной кодовой точке. */
    @Test
    fun acceptsExactLimitsAsUnicodeCodePoints() {
        val title = "📚".repeat(100)
        val description = "🧭".repeat(500)
        val question = "📚".repeat(1_000)
        assertEquals(
            Outcome.Success(DeckText(title, description)),
            MaterialValidator.deck("\t$title ", "\n$description "),
        )
        assertEquals(
            Outcome.Success(CardText(question, question)),
            MaterialValidator.card(question, question),
        )
    }

    /** Валидатор возвращает ошибки обоих полей за один вызов. */
    @Test
    fun reportsBlankTitleAndLongDescription() {
        val error = AppError.InvalidFields(
            mapOf(
                MaterialField.Title to MaterialIssue.Required,
                MaterialField.Description to MaterialIssue.TooLong,
            ),
        )
        assertEquals(
            Outcome.Failure(error),
            MaterialValidator.deck("\u00A0\u0085", "🧭".repeat(501)),
        )
    }

    /** Пределы вопроса и ответа проверяются независимо. */
    @Test
    fun rejectsLongQuestionAndBlankAnswer() {
        val error = AppError.InvalidFields(
            mapOf(
                MaterialField.Question to MaterialIssue.TooLong,
                MaterialField.Answer to MaterialIssue.Required,
            ),
        )
        assertEquals(Outcome.Failure(error), MaterialValidator.card("📚".repeat(1_001), " \n\t"))
    }

    /** Поиск допускает пустую строку и применяет ограничение OpenAPI. */
    @Test
    fun normalizesSearchLikeTheApi() {
        assertEquals(Outcome.Success(""), MaterialValidator.search("\u0085\t"))
        assertEquals(Outcome.Success("Европа"), MaterialValidator.search(" \nЕвропа\u00A0"))
        val error = AppError.InvalidFields(mapOf(MaterialField.Search to MaterialIssue.TooLong))
        assertEquals(Outcome.Failure(error), MaterialValidator.search("📚".repeat(101)))
    }

    /** NUL и повреждённая суррогатная последовательность не уходят в хранилище. */
    @Test
    fun rejectsUnsupportedCharacters() {
        val error = AppError.InvalidFields(
            mapOf(
                MaterialField.Question to MaterialIssue.UnsupportedCharacter,
                MaterialField.Answer to MaterialIssue.UnsupportedCharacter,
            ),
        )
        assertEquals(Outcome.Failure(error), MaterialValidator.card("А\u0000Б", "\uD800"))
    }
}
