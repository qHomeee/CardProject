"""Проверки правил ввода каталога."""

import pytest
from pydantic import ValidationError

from catalog.schemas import CardWrite, DeckWrite


def test_deck_removes_only_outer_whitespace():
    deck = DeckWrite(title="  Столицы  Европы ", description="\tСтрока 1\nСтрока 2\n")
    assert deck.model_dump() == {"title": "Столицы  Европы", "description": "Строка 1\nСтрока 2"}


def test_deck_defaults_to_empty_description():
    assert DeckWrite(title="География").description == ""


def test_deck_accepts_normalized_title_and_description_at_limits():
    title = "Я" * 100
    description = "б" * 500
    deck = DeckWrite(title=f"  {title}\t", description=f"\n{description}  ")
    assert deck.title == title
    assert deck.description == description


@pytest.mark.parametrize(
    ("payload", "field", "error_type"),
    [
        ({"title": "Я" * 101}, "title", "string_too_long"),
        ({"title": "\t \n"}, "title", "string_too_short"),
        ({"title": 123}, "title", "string_type"),
        ({"title": "Набор", "description": "б" * 501}, "description", "string_too_long"),
        ({"title": "Набор", "card_count": 4}, "card_count", "extra_forbidden"),
    ],
    ids=["title-101", "blank-title", "non-string-title", "description-501", "read-only-field"],
)
def test_deck_rejects_invalid_input(payload, field, error_type):
    with pytest.raises(ValidationError) as error:
        DeckWrite.model_validate(payload)
    detail, = error.value.errors()
    assert detail["loc"] == (field,)
    assert detail["type"] == error_type


def test_card_preserves_multiline_text_at_the_limit():
    question = "Первая строка\n" + "Я" * (1000 - len("Первая строка\n"))
    card = CardWrite(question=f"\n{question}  ", answer="  Строка 1\nСтрока 2 ")
    assert card.question == question
    assert card.answer == "Строка 1\nСтрока 2"


@pytest.mark.parametrize(
    ("field", "value", "error_type"),
    [
        ("question", "Начало\x00конец", "value_error"),
        ("answer", "  \n ", "string_too_short"),
        ("answer", "Я" * 1001, "string_too_long"),
    ],
    ids=["nul-in-question", "blank-answer", "answer-1001"],
)
def test_card_rejects_invalid_text(field, value, error_type):
    payload = {"question": "Столица Португалии?", "answer": "Лиссабон"}
    payload[field] = value
    with pytest.raises(ValidationError) as error:
        CardWrite.model_validate(payload)
    detail, = error.value.errors()
    assert detail["loc"] == (field,)
    assert detail["type"] == error_type
