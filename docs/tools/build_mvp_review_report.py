"""Собирает отчёт о ревью Android и проверке реализации MVP."""

from build_design_system import build_document


if __name__ == "__main__":
    build_document(
        stem="24-android-mvp-review",
        footer_title="Ревью Android и MVP",
        title="Ревью кода Запомни и готовность MVP",
        subject="Практика 24",
        widths={"Проверка": [5.6, 11.4]},
    )
