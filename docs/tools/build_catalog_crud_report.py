"""Собирает отчёт практики 9 о базе данных и CRUD."""

from build_design_system import build_document


if __name__ == "__main__":
    build_document(
        stem="09-catalog-crud",
        footer_title="База данных и CRUD",
        title="База данных и CRUD каталога Запомни",
        subject="Практика 9",
        widths={"Операции": [6.2, 10.8], "Проверка": [5.8, 11.2]},
    )
