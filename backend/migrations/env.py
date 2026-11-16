"""Запуск миграций каталога."""

from alembic import context

from catalog.database import database_engine
from catalog.models import Base


def run_migrations() -> None:
    """Выполняет миграции внутри транзакции Alembic."""
    with context.begin_transaction():
        context.run_migrations()


if context.is_offline_mode():
    context.configure(
        dialect_name="postgresql", target_metadata=Base.metadata, literal_binds=True,
    )
    run_migrations()
else:
    engine = database_engine()
    try:
        with engine.connect() as connection:
            context.configure(
                connection=connection, target_metadata=Base.metadata,
                compare_server_default=True,
            )
            run_migrations()
    finally:
        engine.dispose()
