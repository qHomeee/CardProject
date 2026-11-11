"""Собирает DOCX практики 7 из отчёта о прототипе."""

from build_design_system import build_document


if __name__ == '__main__':
    build_document(
        stem='07-prototype', footer_title='Прототип',
        title='Кликабельный прототип приложения Запомни', subject='Практика 7',
        widths={'Действие': [6.2, 10.8]},
    )
