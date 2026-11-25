"""Собирает отчёт о сетевом клиенте каталога из Markdown."""

from build_design_system import build_document


if __name__ == "__main__":
    build_document(
        stem="15-catalog-client",
        footer_title="Сетевой клиент каталога",
        title="Сетевой клиент каталога Запомни",
        subject="Практика 15",
        widths={"Запрос": [6.0, 11.0], "Ошибка": [5.3, 11.7]},
    )
