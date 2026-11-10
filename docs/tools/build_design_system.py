"""Собирает отчёт практики 5 из Markdown. Запуск: python docs/tools/build_design_system.py."""

from pathlib import Path

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt

from build_tz import add_inline, add_table
from document_style import apply_document_style

DOCS = Path(__file__).resolve().parents[1]
WIDTHS = {
    'Роль': [3.8, 6.0, 3.6, 3.6],
    'Стиль': [2.7, 3.2, 4.0, 7.1],
    'Компонент': [4.3, 6.3, 6.4],
}


def insert_table(doc, rows, widths_by_header):
    """Переносит таблицу и задаёт ширину под её содержимое."""
    add_table(doc, rows)
    table = doc.tables[-1]
    widths = widths_by_header[rows[0][0]]
    for column, width in zip(table.columns, widths, strict=True):
        column.width = Cm(width)
    for row in table.rows:
        for cell, width in zip(row.cells, widths, strict=True):
            cell.width = Cm(width)


def build_document(stem='05-design-system', footer_title='Дизайн-система',
                   title='Дизайн-система приложения Запомни', subject='Практика 5',
                   widths=WIDTHS):
    """Переносит текст и ссылки в DOCX с оформлением проекта."""
    doc = Document()
    section = doc.sections[0]
    section.page_width, section.page_height = Cm(21), Cm(29.7)
    section.top_margin = section.bottom_margin = Cm(1.6)
    section.left_margin = section.right_margin = Cm(2)
    doc.styles['Normal'].paragraph_format.space_after = Pt(6)
    heading = doc.styles['Heading 1'].paragraph_format
    heading.space_before = Pt(9)
    heading.space_after = Pt(4)
    heading.keep_with_next = True
    doc.styles['Title'].font.underline = False
    footer = section.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    footer.add_run(f'Запомни    {footer_title}    ')
    page_number = OxmlElement('w:fldSimple')
    page_number.set(qn('w:instr'), 'PAGE')
    footer._p.append(page_number)

    rows = []
    lines = (DOCS / f'{stem}.md').read_text(encoding='utf-8').splitlines()
    for line in [*lines, '']:
        line = line.strip()
        if line.startswith('|'):
            values = [value.strip() for value in line.strip('|').split('|')]
            if not all(value == '---' for value in values):
                rows.append(values)
            continue
        if rows:
            insert_table(doc, rows, widths)
            rows = []
        if line.startswith('# '):
            doc.add_paragraph(line[2:], 'Title')
        elif line.startswith('## '):
            doc.add_heading(line[3:], 1)
        elif line:
            add_inline(doc.add_paragraph(), line)

    doc.core_properties.title = title
    doc.core_properties.subject = subject
    doc.core_properties.author = ''
    doc.core_properties.last_modified_by = ''
    apply_document_style(doc)
    output = DOCS / f'{stem}.docx'
    doc.save(output)
    print(f'Created {output}')


if __name__ == '__main__':
    build_document()
