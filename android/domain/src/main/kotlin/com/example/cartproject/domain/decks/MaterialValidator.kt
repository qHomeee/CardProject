package com.example.cartproject.domain.decks

import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome

/** Поля форм и поиска, для которых применяются ограничения. */
enum class MaterialField { Title, Description, Question, Answer, Search }

/** Причина отклонения поля. */
enum class MaterialIssue { Required, TooLong, UnsupportedCharacter }

/** Поля набора после нормализации. */
data class DeckText(val title: String, val description: String)

/** Поля карточки после нормализации. */
data class CardText(val question: String, val answer: String)

/** Применяет ограничения на длину в кодовых точках Unicode. */
object MaterialValidator {
    /** Проверяет название и описание набора. */
    fun deck(title: String, description: String): Outcome<DeckText> {
        val errors = mutableMapOf<MaterialField, MaterialIssue>()
        val name = validate(title, MaterialField.Title, 100, true, errors)
        val detail = validate(description, MaterialField.Description, 500, false, errors)
        return result(DeckText(name, detail), errors)
    }

    /** Проверяет вопрос и ответ, сохраняя внутренние переносы строк. */
    fun card(question: String, answer: String): Outcome<CardText> {
        val errors = mutableMapOf<MaterialField, MaterialIssue>()
        val prompt = validate(question, MaterialField.Question, 1_000, true, errors)
        val response = validate(answer, MaterialField.Answer, 1_000, true, errors)
        return result(CardText(prompt, response), errors)
    }

    /** Приводит поисковую строку к форме, которую принимает API каталога. */
    fun search(query: String): Outcome<String> {
        val errors = mutableMapOf<MaterialField, MaterialIssue>()
        val text = validate(query, MaterialField.Search, 100, false, errors)
        return result(text, errors)
    }

    private fun validate(
        input: String,
        field: MaterialField,
        limit: Int,
        required: Boolean,
        errors: MutableMap<MaterialField, MaterialIssue>,
    ): String {
        val value = input.trim { it.isWhitespace() || it == '\u0085' }
        val issue = when {
            required && value.isEmpty() -> MaterialIssue.Required
            value.codePoints().anyMatch { it == 0 || it in 0xD800..0xDFFF } ->
                MaterialIssue.UnsupportedCharacter
            value.codePointCount(0, value.length) > limit -> MaterialIssue.TooLong
            else -> null
        }
        if (issue != null) errors[field] = issue
        return value
    }

    private fun <T> result(value: T, errors: Map<MaterialField, MaterialIssue>): Outcome<T> =
        if (errors.isEmpty()) {
            Outcome.Success(value)
        } else {
            Outcome.Failure(AppError.InvalidFields(errors.toMap()))
        }
}
