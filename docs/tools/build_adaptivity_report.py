"""Собирает отчёт практики 23 из Markdown."""

from build_design_system import build_document


if __name__ == "__main__":
    build_document(
        stem="23-adaptive-accessibility",
        footer_title="Адаптивность и доступность",
        title="Адаптивность и доступность приложения Запомни",
        subject="Практика 23",
        widths={"Проверка": [5.4, 11.6]},
    )
