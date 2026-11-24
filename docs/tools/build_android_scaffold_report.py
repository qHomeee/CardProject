"""Собирает отчёт о каркасе Android-приложения из Markdown."""

from build_design_system import build_document


if __name__ == "__main__":
    build_document(
        stem="12-android-scaffold",
        footer_title="Каркас приложения",
        title="Каркас приложения Запомни",
        subject="Практика 12",
        widths={"Часть": [3.0, 7.4, 6.6]},
    )
