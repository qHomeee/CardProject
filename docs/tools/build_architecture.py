"""Собирает отчёт практики 4 из Markdown и готовой диаграммы.

Запуск: python docs/tools/build_architecture.py.
Диаграмму обновляет build_architecture_diagram.py.
"""

import re
from pathlib import Path

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt

from build_architecture_diagram import INSERT_WIDTH_CM
from build_tz import add_inline, add_table
from document_style import apply_document_style

DOCS = Path(__file__).resolve().parents[1]
WIDTHS = {
    'Модель': [4.8, 12.2],
    'Интерфейс': [4.8, 12.2],
    'Экран': [3.6, 7.0, 6.4],
    'Принцип': [2.4, 5.6, 9.0],
}


def insert_table(doc, rows):
    """Подбирает ширину колонок по содержанию таблицы."""
    add_table(doc, rows)
    table = doc.tables[-1]
    widths = WIDTHS[rows[0][0]]
    for column, width in zip(table.columns, widths, strict=True):
        column.width = Cm(width)
    for index, row in enumerate(table.rows):
        for cell, width in zip(row.cells, widths, strict=True):
            cell.width = Cm(width)
            if rows[0][0] == 'Принцип' and index < len(rows) - 1:
                cell.paragraphs[0].paragraph_format.keep_with_next = True


def build_document():
    """Переносит текст, таблицы и диаграмму в DOCX с общим оформлением."""
    doc = Document()
    section = doc.sections[0]
    section.page_width, section.page_height = Cm(21), Cm(29.7)
    section.top_margin = section.bottom_margin = Cm(1.6)
    section.left_margin = section.right_margin = Cm(2)
    doc.styles['Normal'].paragraph_format.space_after = Pt(6)
    doc.styles['Title'].font.underline = False
    for style in ('Heading 1', 'Heading 2'):
        doc.styles[style].paragraph_format.space_before = Pt(9)
        doc.styles[style].paragraph_format.space_after = Pt(4)
        doc.styles[style].paragraph_format.keep_with_next = True

    footer = section.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    footer.add_run('Запомни    Архитектура    ')
    page_number = OxmlElement('w:fldSimple')
    page_number.set(qn('w:instr'), 'PAGE')
    footer._p.append(page_number)

    lines = (DOCS / '04-architecture.md').read_text(encoding='utf-8').splitlines()
    index = 0
    while index < len(lines):
        line = lines[index].strip().replace('`', '')
        index += 1
        if not line:
            continue
        if line.startswith('# '):
            doc.add_paragraph(line[2:], 'Title')
        elif line.startswith('## '):
            doc.add_heading(line[3:], 1)
        elif line.startswith('### '):
            doc.add_heading(line[4:], 2)
        elif line.startswith('!['):
            picture = doc.add_picture(str(DOCS / 'diagrams' / '04-dependencies.png'), width=Cm(INSERT_WIDTH_CM))
            picture._inline.docPr.set('descr', line[2:line.index(']')])
            paragraph = doc.paragraphs[-1]
            paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
            paragraph.paragraph_format.keep_with_next = True
        elif line.startswith('|'):
            index -= 1
            rows = []
            while index < len(lines) and lines[index].startswith('|'):
                values = [part.strip().replace('`', '') for part in lines[index].strip('|').split('|')]
                if not all(re.fullmatch(r'[- :]+', value) for value in values):
                    rows.append(values)
                index += 1
            insert_table(doc, rows)
        else:
            add_inline(doc.add_paragraph(), line)

    doc.core_properties.title = 'Архитектура приложения Запомни'
    doc.core_properties.subject = 'Практика 4'
    doc.core_properties.author = ''
    doc.core_properties.last_modified_by = ''
    apply_document_style(doc)
    output = DOCS / '04-architecture.docx'
    doc.save(output)
    print(f'Created {output}')


if __name__ == '__main__':
    build_document()
