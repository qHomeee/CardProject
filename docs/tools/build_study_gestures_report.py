"""Собирает отчёт практики 20 из Markdown."""

from build_design_system import build_document


if __name__ == "__main__":
    build_document(
        stem="20-study-gestures",
        footer_title="Занятие и жесты",
        title="Занятие и жесты приложения Запомни",
        subject="Практика 20",
        widths={"Действие": [5.0, 12.0]},
    )
