"""Создать таблицы наборов и карточек."""

import sqlalchemy as sa
from alembic import op

revision = "0001_catalog"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    """Создаёт каталог и каскадную связь карточек с набором."""
    op.create_table(
        "decks",
        sa.Column("id", sa.Uuid(), primary_key=True),
        sa.Column("title", sa.String(100), nullable=False),
        sa.Column("description", sa.String(500), nullable=False, server_default=""),
        sa.Column("title_search", sa.Text(collation="C"), nullable=False),
        sa.CheckConstraint("char_length(title) > 0", name="ck_decks_title"),
    )
    op.create_table(
        "cards",
        sa.Column("id", sa.Uuid(), primary_key=True),
        sa.Column("deck_id", sa.Uuid(), nullable=False),
        sa.Column("question", sa.String(1000), nullable=False),
        sa.Column("answer", sa.String(1000), nullable=False),
        sa.CheckConstraint("char_length(question) > 0", name="ck_cards_question"),
        sa.CheckConstraint("char_length(answer) > 0", name="ck_cards_answer"),
        sa.ForeignKeyConstraint(
            ["deck_id"], ["decks.id"], name="fk_cards_deck_id", ondelete="CASCADE",
        ),
    )
    op.create_index("ix_cards_deck_id", "cards", ["deck_id"])


def downgrade() -> None:
    """Удаляет таблицы каталога вместе с данными."""
    op.drop_table("cards")
    op.drop_table("decks")
