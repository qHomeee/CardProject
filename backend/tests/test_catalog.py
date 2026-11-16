"""Проверки HTTP-контракта на изолированных схемах PostgreSQL."""

import os
import secrets
from pathlib import Path
from uuid import uuid4

import pytest
from alembic import command
from alembic.config import Config
from fastapi.openapi.utils import get_openapi
from fastapi.testclient import TestClient
from sqlalchemy import func, select
from sqlalchemy.schema import CreateSchema, DropSchema

from catalog.database import database_engine
from catalog.main import create_app
from catalog.models import Card

DECKS = "/api/v1/decks"


@pytest.fixture
def catalog_database(monkeypatch):
    """Применяет миграцию в отдельной схеме и удаляет эту схему после теста."""
    url = os.getenv("TEST_DATABASE_URL")
    if not url:
        pytest.skip("Укажите TEST_DATABASE_URL выделенной тестовой базы PostgreSQL.")
    schema = f"catalog_test_{uuid4().hex}"
    engine = database_engine(url)
    options = f"{engine.url.query.get('options', '')} -c search_path={schema}".strip()
    scoped_url = engine.url.update_query_dict({"options": options}).render_as_string(hide_password=False)

    try:
        with engine.begin() as connection:
            connection.execute(CreateSchema(schema))
        try:
            monkeypatch.setenv("DATABASE_URL", scoped_url)
            command.upgrade(Config(str(Path(__file__).resolve().parents[1] / "alembic.ini")), "head")
            yield scoped_url
        finally:
            with engine.begin() as connection:
                connection.execute(DropSchema(schema, cascade=True))
    finally:
        engine.dispose()


@pytest.fixture
def client(catalog_database, monkeypatch):
    """Создаёт временный ключ и клиент с рабочим пулом тестовой базы."""
    key = secrets.token_urlsafe(32)
    monkeypatch.setenv("EDITOR_API_KEY", key)
    with TestClient(create_app()) as api:
        yield api, {"X-Editor-Key": key}


def create_deck(api, headers, title="Столицы Европы"):
    result = api.post(DECKS, headers=headers, json={"title": title, "description": "  География  "})
    assert result.status_code == 201
    path = result.headers["Location"]
    deck = result.json()
    assert path == f"{DECKS}/{deck['id']}"
    assert deck["cards"] == []
    return path


def add_card(api, headers, deck, question="Столица Португалии?", answer="Лиссабон"):
    result = api.post(f"{deck}/cards", headers=headers, json={"question": question, "answer": answer})
    assert result.status_code == 201
    card = result.json()
    assert result.headers["Location"] == f"{deck}/cards/{card['id']}"
    return result.headers["Location"], card


def test_full_catalog_crud(client):
    api, editor = client
    deck = create_deck(api, editor, "  Столицы Европы  ")
    card_path, card = add_card(api, editor, deck)
    assert api.get(card_path).json() == card
    assert api.get(deck).json()["cards"] == [card]
    listing = api.get(DECKS).json()["items"]
    assert len(listing) == 1 and listing[0]["card_count"] == 1

    updated = api.put(deck, headers=editor, json={"title": "  Европа  "})
    assert updated.status_code == 200
    assert updated.json() == {
        "id": listing[0]["id"], "title": "Европа", "description": "", "cards": [card],
    }
    assert api.get(DECKS, params={"q": "столицы"}).json() == {"items": []}
    assert api.get(DECKS, params={"q": "  ЕВРОПА "}).json()["items"][0]["id"] == listing[0]["id"]

    updated_card = api.put(card_path, headers=editor, json={"question": "Столица Испании?", "answer": "Мадрид"})
    assert updated_card.status_code == 200
    assert updated_card.json() == {**card, "question": "Столица Испании?", "answer": "Мадрид"}

    deleted_card = api.delete(card_path, headers=editor)
    assert deleted_card.status_code == 204 and deleted_card.content == b""
    assert api.get(deck).json()["cards"] == []
    assert api.get(card_path).json()["error"]["code"] == "card_not_found"
    deleted_deck = api.delete(deck, headers=editor)
    assert deleted_deck.status_code == 204 and deleted_deck.content == b""
    assert api.get(deck).json()["error"]["code"] == "deck_not_found"


def test_cascade_and_card_scope(client, catalog_database):
    api, editor = client
    source = create_deck(api, editor)
    another = create_deck(api, editor)
    card_path, card = add_card(api, editor, source)
    _, second_card = add_card(api, editor, source, "Столица Испании?", "Мадрид")
    snapshot = api.get(source).json()
    assert snapshot["cards"] == sorted([card, second_card], key=lambda item: item["id"])
    assert snapshot["description"] == "География"
    foreign_path = f"{another}/cards/{card['id']}"
    for method in ("GET", "PUT", "DELETE"):
        request = {"json": {"question": "Вопрос", "answer": "Ответ"}} if method == "PUT" else {}
        result = api.request(method, foreign_path, headers=editor, **request)
        assert result.status_code == 404 and result.json()["error"]["code"] == "card_not_found"

    assert api.delete(source, headers=editor).status_code == 204
    assert api.get(card_path).status_code == 404
    assert api.get(another).status_code == 200
    engine = database_engine(catalog_database)
    try:
        with engine.connect() as connection:
            assert connection.scalar(select(func.count()).select_from(Card)) == 0
    finally:
        engine.dispose()


def test_all_writes_reject_missing_wrong_and_duplicate_key(client):
    api, editor = client
    deck = create_deck(api, editor)
    card, original = add_card(api, editor, deck)
    deck_data = {"title": "Другой"}
    card_data = {"question": "Другой вопрос", "answer": "Другой ответ"}
    writes = [
        ("POST", DECKS, deck_data), ("PUT", deck, deck_data), ("DELETE", deck, None),
        ("POST", f"{deck}/cards", card_data), ("PUT", card, card_data), ("DELETE", card, None),
    ]
    for identity in ({}, {"X-Editor-Key": "неверный".encode("utf-8")},
                     [*editor.items(), ("X-Editor-Key", "duplicate")]):
        for method, path, body in writes:
            request = {"json": body} if body is not None else {}
            result = api.request(method, path, headers=identity, **request)
            assert result.status_code == 401
            assert result.headers["WWW-Authenticate"] == "APIKey"
            assert result.json() == {"error": {"code": "editor_required", "message": "Требуется доступ редактора."}}
    assert api.get(card).json() == original
    assert api.get(deck).json()["title"] == "Столицы Европы"
    assert len(api.get(DECKS).json()["items"]) == 1


def test_writes_fail_closed_when_key_is_unset(catalog_database, monkeypatch):
    monkeypatch.delenv("EDITOR_API_KEY", raising=False)
    with TestClient(create_app()) as api:
        assert api.get(DECKS).json() == {"items": []}
        result = api.post(DECKS, headers={"X-Editor-Key": "anything"}, json={"title": "Набор"})
        assert result.status_code == 503
        assert result.json()["error"]["code"] == "service_unavailable"
        assert api.get(DECKS).json() == {"items": []}


def test_unicode_search_is_literal_and_preserves_duplicates(client):
    api, editor = client
    first = create_deck(api, editor, "  Straße 50%_ ")
    second = create_deck(api, editor, "Straße 50%_")
    create_deck(api, editor, "Strasse 50xa")
    ids = sorted([first.rsplit("/", 1)[1], second.rsplit("/", 1)[1]])
    for term in ["  STRASSE 50%_ ", "%", "_"]:
        result = api.get(DECKS, params={"q": term})
        assert result.status_code == 200
        assert [deck["id"] for deck in result.json()["items"]] == ids
    assert len(api.get(DECKS, params={"q": "   "}).json()["items"]) == 3
    assert api.get(DECKS, params={"q": "нет совпадения"}).json() == {"items": []}


def test_validation_after_trim_and_multiline_card(client):
    api, editor = client
    for value in ["  \t  ", "Я" * 101, "\x00", 123]:
        result = api.post(DECKS, headers=editor, json={"title": value})
        assert result.status_code == 422
        error = result.json()["error"]
        assert error["code"] == "validation_error"
        assert error["fields"][0]["field"] == "body.title"
    assert api.get(DECKS, params={"q": "Я" * 101}).status_code == 422
    assert api.get(f"{DECKS}/wrong-id").status_code == 422
    assert api.post(DECKS, headers=editor, json={"title": "Набор", "cards": []}).status_code == 422
    deck = create_deck(api, editor, f"   {'Я' * 100}   ")
    _, card = add_card(api, editor, deck, "  Первая строка\nВторая строка  ", "  Ответ  ")
    assert card["question"] == "Первая строка\nВторая строка"
    assert card["answer"] == "Ответ"


def test_published_openapi_matches_routes_and_security():
    app = create_app()
    published = app.openapi()
    generated = get_openapi(title=app.title, version=app.version, routes=app.routes)
    allowed_methods = {"get", "post", "put", "delete"}
    declared = {
        (path, method): info
        for path, operations in published["paths"].items()
        for method, info in operations.items() if method in allowed_methods
    }
    actual = {
        (path, method): info
        for path, operations in generated["paths"].items()
        for method, info in operations.items() if method in allowed_methods
    }
    assert len(declared) == 9 and declared.keys() == actual.keys()
    for endpoint, operation in declared.items():
        runtime = actual[endpoint]
        assert runtime["operationId"] == operation["operationId"]
        assert runtime.get("security", []) == operation.get("security", [])
        expected_success = {status for status in operation["responses"] if status.startswith("2")}
        assert expected_success == {status for status in runtime["responses"] if status.startswith("2")}
