"""Собирает отчёт практики 11 о тестировании сервера."""

from build_design_system import build_document


if __name__ == "__main__":
    build_document(
        stem="11-backend-tests",
        footer_title="Тестирование сервера",
        title="Тестирование сервера каталога Запомни",
        subject="Практика 11",
        widths={"ID": [1.5, 7.2, 8.3]},
    )
