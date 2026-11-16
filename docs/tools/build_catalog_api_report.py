"""Собирает отчёт практики 8 о контракте каталога."""

from build_design_system import build_document


if __name__ == '__main__':
    build_document(
        stem='08-catalog-api', footer_title='API каталога',
        title='Проектирование API каталога Запомни', subject='Практика 8',
        widths={'Путь': [7.1, 9.9]},
    )
