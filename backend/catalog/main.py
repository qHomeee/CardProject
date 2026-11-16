"""Точка входа FastAPI для каталога."""

import os
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Any

import yaml
from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from sqlalchemy.exc import DBAPIError, TimeoutError

from catalog.database import database_engine
from catalog.errors import (
    CatalogError,
    catalog_error_handler,
    database_error_handler,
    internal_error_handler,
    validation_error_handler,
)
from catalog.routes import router

CONTRACT = Path(__file__).resolve().parents[1] / "openapi.yaml"


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    """Настраивает пул PostgreSQL и редакторский доступ из окружения."""
    engine = database_engine()
    app.state.engine = engine
    app.state.editor_key = os.getenv("EDITOR_API_KEY")
    try:
        yield
    finally:
        engine.dispose()


def create_app() -> FastAPI:
    """Регистрирует маршруты, ошибки и опубликованный контракт каталога."""
    app = FastAPI(title="Каталог наборов «Запомни»", version="0.1.0", lifespan=lifespan)
    app.include_router(router)
    app.add_exception_handler(CatalogError, catalog_error_handler)
    app.add_exception_handler(RequestValidationError, validation_error_handler)
    app.add_exception_handler(DBAPIError, database_error_handler)
    app.add_exception_handler(TimeoutError, database_error_handler)
    app.add_exception_handler(Exception, internal_error_handler)

    def openapi() -> dict[str, Any]:
        if app.openapi_schema is None:
            app.openapi_schema = yaml.safe_load(CONTRACT.read_text(encoding="utf-8"))
        return app.openapi_schema

    app.openapi = openapi
    return app


app = create_app()
