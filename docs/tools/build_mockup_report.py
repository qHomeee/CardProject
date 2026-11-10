"""Собирает DOCX практики 6 из отчёта о макетах."""

from build_design_system import build_document


if __name__ == '__main__':
    build_document(
        stem='06-mockups', footer_title='Макеты',
        title='Макеты экранов приложения Запомни', subject='Практика 6',
        widths={'Экран': [4.1, 9.8, 3.1]},
    )
