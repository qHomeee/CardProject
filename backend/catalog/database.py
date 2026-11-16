"""Подключение каталога к PostgreSQL."""

import os

from sqlalchemy import create_engine
from sqlalchemy.engine import Engine, make_url


def database_engine(url: str | None = None) -> Engine:
    """Создаёт пул соединений; подключение устанавливается по первому запросу."""
    connection = url if url is not None else os.getenv("DATABASE_URL")
    if not connection:
        raise RuntimeError("Укажите DATABASE_URL для PostgreSQL.")

    target = make_url(connection)
    if target.drivername not in {"postgresql", "postgresql+psycopg"}:
        raise RuntimeError("DATABASE_URL должен указывать на PostgreSQL с Psycopg.")
    return create_engine(
        target.set(drivername="postgresql+psycopg"),
        connect_args={"connect_timeout": 5},
        hide_parameters=True,
        pool_pre_ping=True,
    )
