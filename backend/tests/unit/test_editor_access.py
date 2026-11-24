"""Проверки редакторского доступа без запросов к приложению."""

import pytest
from fastapi import FastAPI, Request

from catalog.errors import CatalogError
from catalog.routes import require_editor

EDITOR_KEY = "local-unit-test-editor-key"


def editor_request(expected: str | None, values: list[str]) -> Request:
    app = FastAPI()
    app.state.editor_key = expected
    return Request({
        "type": "http",
        "app": app,
        "headers": [(b"x-editor-key", header.encode("ascii")) for header in values],
    })


def test_editor_accepts_one_matching_header():
    request = editor_request(EDITOR_KEY, [EDITOR_KEY])
    assert require_editor(request, EDITOR_KEY) is None


@pytest.mark.parametrize(
    ("values", "key"),
    [
        ([], None),
        (["incorrect-key"], "incorrect-key"),
        ([EDITOR_KEY, EDITOR_KEY], EDITOR_KEY),
    ],
    ids=["missing", "wrong", "duplicate"],
)
def test_editor_rejects_missing_wrong_or_duplicate_key(values, key):
    request = editor_request(EDITOR_KEY, values)
    with pytest.raises(CatalogError) as error:
        require_editor(request, key)
    assert (error.value.status, error.value.code) == (401, "editor_required")
    assert EDITOR_KEY not in str(error.value)


def test_editor_fails_closed_without_configuration():
    request = editor_request(None, [EDITOR_KEY])
    with pytest.raises(CatalogError) as error:
        require_editor(request, EDITOR_KEY)
    assert (error.value.status, error.value.code) == (503, "service_unavailable")
