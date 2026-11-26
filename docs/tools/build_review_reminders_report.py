"""Собирает отчёт практики 22 из Markdown."""

from build_design_system import build_document


if __name__ == "__main__":
    build_document(
        stem="22-review-reminders",
        footer_title="Напоминания о повторении",
        title="Напоминания о повторении карточек в Запомни",
        subject="Практика 22",
        widths={"Проверка": [5.8, 11.2]},
    )
