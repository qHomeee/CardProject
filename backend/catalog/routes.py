"""HTTP-операции для наборов и карточек."""

import secrets
from collections.abc import Iterator
from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, Query, Request, Response, Security
from fastapi.security import APIKeyHeader
from sqlalchemy import func, select
from sqlalchemy.orm import Session, joinedload

from catalog.errors import CatalogError
from catalog.models import Card, Deck
from catalog.schemas import CardView, CardWrite, DeckDetail, DeckList, DeckSummary, DeckWrite, SearchText

router = APIRouter(prefix="/api/v1")
editor_header = APIKeyHeader(name="X-Editor-Key", scheme_name="EditorKey", auto_error=False)


def require_editor(request: Request, key: Annotated[str | None, Security(editor_header)]) -> None:
    """Разрешает изменение только по одному заголовку с действительным ключом."""
    expected = request.app.state.editor_key
    if not expected or not expected.strip():
        raise CatalogError(503, "service_unavailable", "Операция временно недоступна.")
    supplied = request.headers.getlist(editor_header.model.name)
    if key is None or len(supplied) != 1 or not secrets.compare_digest(
        key.encode("utf-8"), expected.encode("utf-8"),
    ):
        raise CatalogError(401, "editor_required", "Требуется доступ редактора.")


def get_session(request: Request) -> Iterator[Session]:
    """Открывает отдельную сессию SQLAlchemy на запрос."""
    with Session(request.app.state.engine) as session:
        yield session


Database = Annotated[Session, Depends(get_session)]
EDITOR_REQUIRED = [Depends(require_editor)]


def deck_record(session: Session, deck_id: UUID, *, lock: bool = False) -> Deck:
    """Возвращает набор, при записи блокируя его на время транзакции."""
    statement = select(Deck).where(Deck.id == deck_id)
    if lock:
        statement = statement.with_for_update()
    deck = session.scalar(statement)
    if deck is None:
        raise CatalogError(404, "deck_not_found", "Набор не найден.")
    return deck


def deck_snapshot(session: Session, deck_id: UUID) -> DeckDetail:
    """Читает набор и карточки одним SQL-запросом из одного снимка базы."""
    statement = select(Deck).options(joinedload(Deck.cards)).where(Deck.id == deck_id)
    deck = session.scalars(statement).unique().one_or_none()
    if deck is None:
        raise CatalogError(404, "deck_not_found", "Набор не найден.")
    return DeckDetail.model_validate(deck)


def card_record(session: Session, deck_id: UUID, card_id: UUID, *, lock: bool = False) -> Card:
    """Находит карточку только внутри указанного набора."""
    statement = select(Card).where(Card.deck_id == deck_id, Card.id == card_id)
    if lock:
        statement = statement.with_for_update()
    card = session.scalar(statement)
    if card is None:
        raise CatalogError(404, "card_not_found", "Карточка в этом наборе не найдена.")
    return card


@router.get("/decks", operation_id="listDecks", tags=["Наборы"])
def list_decks(session: Database, q: Annotated[SearchText, Query()] = "") -> DeckList:
    """Ищет буквальную подстроку в нормализованных названиях наборов."""
    statement = (
        select(Deck.id, Deck.title, Deck.description, func.count(Card.id).label("card_count"))
        .outerjoin(Card)
        .group_by(Deck.id)
        .order_by(Deck.title_search, Deck.id)
    )
    if q:
        statement = statement.where(func.strpos(Deck.title_search, q.casefold()) > 0)
    rows = session.execute(statement).mappings()
    return DeckList(items=[DeckSummary.model_validate(row) for row in rows])


@router.get("/decks/{deck_id}", operation_id="getDeck", tags=["Наборы"])
def get_deck(deck_id: UUID, session: Database) -> DeckDetail:
    """Возвращает набор для просмотра и скачивания."""
    return deck_snapshot(session, deck_id)


@router.post(
    "/decks", operation_id="createDeck", tags=["Наборы"], status_code=201,
    dependencies=EDITOR_REQUIRED,
)
def create_deck(data: DeckWrite, request: Request, response: Response, session: Database) -> DeckDetail:
    """Сохраняет пустой набор и возвращает адрес его чтения."""
    with session.begin():
        deck = Deck(title=data.title, description=data.description, title_search=data.title.casefold(), cards=[])
        session.add(deck)
        session.flush()
        result = DeckDetail.model_validate(deck)
    response.headers["Location"] = str(request.url_for("get_deck", deck_id=result.id).path)
    return result


@router.put("/decks/{deck_id}", operation_id="updateDeck", tags=["Наборы"], dependencies=EDITOR_REQUIRED)
def update_deck(deck_id: UUID, data: DeckWrite, session: Database) -> DeckDetail:
    """Обновляет поля набора и возвращает его содержимое."""
    with session.begin():
        deck = deck_record(session, deck_id, lock=True)
        deck.title = data.title
        deck.description = data.description
        deck.title_search = data.title.casefold()
        session.flush()
        return deck_snapshot(session, deck_id)


@router.delete(
    "/decks/{deck_id}", operation_id="deleteDeck", tags=["Наборы"], status_code=204,
    dependencies=EDITOR_REQUIRED,
)
def delete_deck(deck_id: UUID, session: Database) -> Response:
    """Удаляет набор, PostgreSQL каскадно удаляет карточки."""
    with session.begin():
        session.delete(deck_record(session, deck_id, lock=True))
    return Response(status_code=204)


@router.get("/decks/{deck_id}/cards/{card_id}", operation_id="getCard", tags=["Карточки"])
def get_card(deck_id: UUID, card_id: UUID, session: Database) -> CardView:
    """Читает карточку выбранного набора."""
    return CardView.model_validate(card_record(session, deck_id, card_id))


@router.post(
    "/decks/{deck_id}/cards", operation_id="createCard", tags=["Карточки"], status_code=201,
    dependencies=EDITOR_REQUIRED,
)
def create_card(
    deck_id: UUID, data: CardWrite, request: Request, response: Response, session: Database,
) -> CardView:
    """Добавляет карточку и возвращает адрес её чтения."""
    with session.begin():
        deck_record(session, deck_id, lock=True)
        card = Card(deck_id=deck_id, question=data.question, answer=data.answer)
        session.add(card)
        session.flush()
        result = CardView.model_validate(card)
    response.headers["Location"] = str(request.url_for("get_card", deck_id=deck_id, card_id=result.id).path)
    return result


@router.put(
    "/decks/{deck_id}/cards/{card_id}", operation_id="updateCard", tags=["Карточки"],
    dependencies=EDITOR_REQUIRED,
)
def update_card(deck_id: UUID, card_id: UUID, data: CardWrite, session: Database) -> CardView:
    """Заменяет вопрос и ответ карточки в выбранном наборе."""
    with session.begin():
        card = card_record(session, deck_id, card_id, lock=True)
        card.question = data.question
        card.answer = data.answer
        return CardView.model_validate(card)


@router.delete(
    "/decks/{deck_id}/cards/{card_id}", operation_id="deleteCard", tags=["Карточки"], status_code=204,
    dependencies=EDITOR_REQUIRED,
)
def delete_card(deck_id: UUID, card_id: UUID, session: Database) -> Response:
    """Удаляет карточку только из указанного набора."""
    with session.begin():
        session.delete(card_record(session, deck_id, card_id, lock=True))
    return Response(status_code=204)
