"""Собирает отчёт практики 21 из Markdown."""

from build_design_system import build_document


if __name__ == "__main__":
    build_document(
        stem="21-gesture-device-check",
        footer_title="Проверка жестов",
        title="Проверка жестов Запомни на Android-телефоне",
        subject="Практика 21",
        widths={"Проверка": [6.2, 10.8]},
    )
