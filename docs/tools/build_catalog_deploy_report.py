"""Собирает отчёт практики 10 о проверках и развёртывании каталога."""

from build_design_system import build_document


if __name__ == "__main__":
    build_document(
        stem="10-catalog-deploy",
        footer_title="Развёртывание каталога",
        title="Ошибки API и развёртывание каталога Запомни",
        subject="Практика 10",
        widths={"Код HTTP": [3.4, 13.6], "Проверка": [5.6, 11.4]},
    )
