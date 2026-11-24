"""Проверки ответов API при недопустимых данных и сбоях."""

import secrets

import pytest
from fastapi.testclient import TestClient
from sqlalchemy.exc import OperationalError, ProgrammingError

from catalog.main import create_app
from catalog.routes import get_session

DECKS = "/api/v1/decks"


@pytest.fixture
def isolated_api(monkeypatch):
    """Конфигурирует приложение без подключения к внешней базе."""
    monkeypatch.setenv("DATABASE_URL", "postgresql+psycopg://test@127.0.0.1:1/catalog_test")
    editor_key = secrets.token_urlsafe(32)
    monkeypatch.setenv("EDITOR_API_KEY", editor_key)
    return create_app(), {"X-Editor-Key": editor_key}


def test_catalog_http_errors_preserve_status_and_headers(isolated_api):
    app, _ = isolated_api
    with TestClient(app) as client:
        response = client.get("/api/v1/missing")
        assert response.status_code == 404
        assert response.json()["error"]["code"] == "route_not_found"
        response = client.patch(DECKS)
        assert response.status_code == 405
        assert response.json()["error"]["code"] == "method_not_allowed"
        assert "GET" in response.headers["Allow"]


def test_validation_explains_length_and_malformed_json(isolated_api):
    app, editor = isolated_api
    with TestClient(app) as client:
        response = client.post(DECKS, headers=editor, json={"title": "Я" * 101})
        assert response.status_code == 422
        assert response.json()["error"]["fields"] == [
            {"field": "body.title", "message": "Допустимо не более 100 символов."},
        ]
        response = client.post(
            DECKS,
            headers={**editor, "Content-Type": "application/json"},
            content='{"title":',
        )
        assert response.status_code == 422
        assert response.json()["error"]["fields"] == [
            {"field": "body", "message": "Исправьте синтаксис JSON."},
        ]


@pytest.mark.parametrize(
    ("exception_type", "status", "code"),
    [
        (OperationalError, 503, "service_unavailable"),
        (ProgrammingError, 500, "internal_error"),
    ],
)
def test_db_errors_hide_internal_details(isolated_api, exception_type, status, code):
    app, _ = isolated_api

    def failed_session():
        raise exception_type("sensitive-sql", {}, RuntimeError("sensitive-details"))

    app.dependency_overrides[get_session] = failed_session
    with TestClient(app) as client:
        response = client.get(DECKS)
        assert response.status_code == status
        assert response.json()["error"]["code"] == code
        assert "sensitive" not in response.text
