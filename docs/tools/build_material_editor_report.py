"""Собирает отчёт практики 19 из Markdown."""

from build_design_system import build_document


if __name__ == "__main__":
    build_document(
        stem="19-material-editor",
        footer_title="Редактор наборов",
        title="Редактор наборов и карточек приложения Запомни",
        subject="Практика 19",
        widths={"Поле": [5.3, 11.7], "Действие": [7.6, 9.4]},
    )
