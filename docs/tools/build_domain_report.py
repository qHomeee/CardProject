"""Собирает отчёт о доменной логике из Markdown."""

from build_design_system import build_document


if __name__ == "__main__":
    build_document(
        stem="14-domain",
        footer_title="Доменная логика",
        title="Доменная логика приложения Запомни",
        subject="Практика 14",
        widths={"Сценарий": [3.7, 13.3], "Оценка": [7.5, 3.4, 6.1], "Группа": [5.2, 2.1, 9.7]},
    )
