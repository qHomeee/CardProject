"""Собирает отчёт о темах и компонентах Compose из Markdown."""

from build_design_system import build_document


if __name__ == "__main__":
    build_document(
        stem="13-android-design-system",
        footer_title="Тема и компоненты",
        title="Тема и компоненты приложения Запомни",
        subject="Практика 13",
        widths={"Компонент": [3.7, 5.9, 7.4]},
    )
