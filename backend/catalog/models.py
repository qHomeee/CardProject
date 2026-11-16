"""Таблицы общего каталога."""

from uuid import UUID, uuid4

from sqlalchemy import CheckConstraint, ForeignKey, String, Text
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship


class Base(DeclarativeBase):
    """Метаданные для ORM и Alembic."""


class Deck(Base):
    """Содержимое и нормализованное название набора."""

    __tablename__ = "decks"
    __table_args__ = (CheckConstraint("char_length(title) > 0", name="ck_decks_title"),)

    id: Mapped[UUID] = mapped_column(primary_key=True, default=uuid4)
    title: Mapped[str] = mapped_column(String(100))
    description: Mapped[str] = mapped_column(String(500), server_default="")
    title_search: Mapped[str] = mapped_column(Text(collation="C"))

    cards: Mapped[list["Card"]] = relationship(
        order_by=lambda: Card.id, passive_deletes=True, cascade="all, delete",
    )


class Card(Base):
    """Вопрос и ответ, принадлежащие одному набору."""

    __tablename__ = "cards"
    __table_args__ = (
        CheckConstraint("char_length(question) > 0", name="ck_cards_question"),
        CheckConstraint("char_length(answer) > 0", name="ck_cards_answer"),
    )

    id: Mapped[UUID] = mapped_column(primary_key=True, default=uuid4)
    deck_id: Mapped[UUID] = mapped_column(
        ForeignKey("decks.id", name="fk_cards_deck_id", ondelete="CASCADE"),
        index=True,
    )
    question: Mapped[str] = mapped_column(String(1000))
    answer: Mapped[str] = mapped_column(String(1000))
