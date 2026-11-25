"""Собирает отчёт практики 17 из Markdown."""

from build_design_system import build_document


if __name__ == "__main__":
    build_document(
        stem="17-catalog-navigation",
        footer_title="Навигация и каталог",
        title="Навигация и каталог приложения Запомни",
        subject="Практика 17",
        widths={"Вкладка": [4.3, 12.7], "Действие": [7.2, 9.8]},
    )
