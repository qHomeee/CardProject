"""Собирает тест-план Android из Markdown."""

from build_design_system import build_document


if __name__ == "__main__":
    build_document(
        stem="25-android-test-plan",
        footer_title="Тестирование Android",
        title="Тестирование приложения Запомни на Android",
        subject="Практика 25",
        widths={"Кейс": [3.1, 7.05, 6.85], "Дефект": [2.8, 6.5, 7.7]},
    )
