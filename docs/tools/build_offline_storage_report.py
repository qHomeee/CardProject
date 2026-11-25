"""Собирает отчёт практики 16 из Markdown."""

from build_design_system import build_document


if __name__ == "__main__":
    build_document(
        stem="16-offline-storage",
        footer_title="Локальное хранение",
        title="Локальное хранение приложения Запомни",
        subject="Практика 16",
        widths={"Таблица": [3.1, 13.9]},
    )
