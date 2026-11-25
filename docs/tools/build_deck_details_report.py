"""Собирает отчёт практики 18 из Markdown."""

from build_design_system import build_document


if __name__ == "__main__":
    build_document(
        stem="18-deck-details",
        footer_title="Просмотр и скачивание",
        title="Просмотр и скачивание наборов Запомни",
        subject="Практика 18",
        widths={"Источник": [4.0, 13.0], "Действие": [6.8, 10.2]},
    )
