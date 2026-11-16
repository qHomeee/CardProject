"""Модели запросов и ответов каталога."""

from typing import Annotated
from uuid import UUID

from pydantic import BaseModel, BeforeValidator, ConfigDict, StringConstraints


def normalize_text(value: object) -> object:
    """Удаляет внешние пробелы и проверяет, что текст можно записать в PostgreSQL."""
    if isinstance(value, str):
        if "\x00" in value:
            raise ValueError("Недопустимый символ.")
        value.encode("utf-8")
        return value.strip()
    return value


Title = Annotated[str, StringConstraints(strict=True, min_length=1, max_length=100), BeforeValidator(normalize_text)]
Description = Annotated[str, StringConstraints(strict=True, max_length=500), BeforeValidator(normalize_text)]
CardText = Annotated[str, StringConstraints(strict=True, min_length=1, max_length=1000), BeforeValidator(normalize_text)]
SearchText = Annotated[str, StringConstraints(strict=True, max_length=100), BeforeValidator(normalize_text)]


class DeckWrite(BaseModel):
    """Редактируемые поля набора."""

    model_config = ConfigDict(extra="forbid")
    title: Title
    description: Description = ""


class CardWrite(BaseModel):
    """Редактируемые поля карточки."""

    model_config = ConfigDict(extra="forbid")
    question: CardText
    answer: CardText


class CardView(BaseModel):
    """Карточка каталога."""

    model_config = ConfigDict(from_attributes=True)
    id: UUID
    question: str
    answer: str


class DeckView(BaseModel):
    """Общие поля набора."""

    model_config = ConfigDict(from_attributes=True)
    id: UUID
    title: str
    description: str


class DeckSummary(DeckView):
    """Набор в результатах поиска."""

    card_count: int


class DeckDetail(DeckView):
    """Набор со всеми карточками для просмотра и скачивания."""

    cards: list[CardView]


class DeckList(BaseModel):
    """Результат поиска в каталоге."""

    items: list[DeckSummary]
