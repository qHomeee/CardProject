"""Проверки формата ошибок и классификации сбоев базы."""

from asyncio import run
from json import loads

import pytest
from fastapi import Request
from psycopg.errors import ConnectionFailure, UndefinedTable
from sqlalchemy.exc import OperationalError, ProgrammingError, TimeoutError

from catalog.errors import database_error_handler, error_response


def test_validation_response_exposes_only_expected_fields():
    fields = [{"field": "body.title", "message": "Введите значение."}]
    response = error_response(422, "validation_error", "Проверьте поля запроса.", fields)
    assert response.status_code == 422
    assert response.headers["content-type"] == "application/json"
    assert loads(response.body) == {
        "error": {
            "code": "validation_error",
            "message": "Проверьте поля запроса.",
            "fields": fields,
        },
    }


def test_editor_error_includes_authentication_challenge():
    response = error_response(401, "editor_required", "Требуется доступ редактора.")
    assert response.headers["www-authenticate"] == "APIKey"
    assert "fields" not in loads(response.body)["error"]


@pytest.mark.parametrize(
    ("failure", "status", "code"),
    [
        (TimeoutError("private-connection-details"), 503, "service_unavailable"),
        (
            OperationalError("private-sql", {}, ConnectionFailure("private-connection-details")),
            503,
            "service_unavailable",
        ),
        (
            ProgrammingError("private-sql", {}, UndefinedTable("private-db-details")),
            500,
            "internal_error",
        ),
    ],
    ids=["pool-timeout", "connection-failure", "programming-error"],
)
def test_db_errors_have_safe_status_and_body(failure, status, code):
    response = run(database_error_handler(Request({"type": "http"}), failure))
    assert response.status_code == status
    assert loads(response.body)["error"]["code"] == code
    assert b"private-" not in response.body
