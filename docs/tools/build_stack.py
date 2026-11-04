"""Собирает DOCX практики 3 из docs/03-stack.md.

Запуск: python docs/tools/build_stack.py. Зависимость: python-docx;
общие функции Markdown и таблиц импортируются из build_tz.py.
"""

from pathlib import Path
import re

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.opc.constants import RELATIONSHIP_TYPE as RT
from docx.shared import Cm, Pt, RGBColor

from build_tz import add_inline, add_table
from document_style import apply_document_style, FONT_NAME

DOCS = Path(__file__).resolve().parents[1]
WIDTHS = {
    'Компонент': [4.0, 5.0, 8.0],
    'Задача': [3.2, 4.8, 9.0],
    'Критерий': [3.4, 6.8, 6.8],
    'Риск': [3.5, 4.7, 8.8],
    'Практика': [3.1, 4.6, 9.3],
}


def build_document():
    """Создаёт документ с повторяемыми шапками и ссылками на источники."""
    doc = Document()
    section = doc.sections[0]
    section.page_width, section.page_height = Cm(21), Cm(29.7)
    section.top_margin = section.bottom_margin = Cm(1.6)
    section.left_margin = section.right_margin = Cm(2)
    for name in ['Normal', 'Title', 'Subtitle', 'Heading 1', 'Heading 2']:
        style = doc.styles[name]
        style.font.name = FONT_NAME
        style.font.color.rgb = RGBColor(0, 0, 0)
        style.paragraph_format.space_after = Pt(6)
    for style in doc.styles:
        for border in list(style.element.iter(qn('w:pBdr'))):
            border.getparent().remove(border)
    doc.styles['Normal'].font.size = Pt(14)
    doc.styles['Normal'].paragraph_format.line_spacing = 1.04
    doc.styles['Title'].font.size = Pt(14)
    doc.styles['Title'].font.underline = False
    doc.styles['Heading 1'].font.size = Pt(14)
    doc.styles['Heading 1'].paragraph_format.space_before = Pt(9)
    doc.styles['Heading 1'].paragraph_format.keep_with_next = True
    footer = section.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    footer.add_run('Запомни    Обоснование стека    ').font.size = Pt(14)
    field = OxmlElement('w:fldSimple')
    field.set(qn('w:instr'), 'PAGE')
    footer._p.append(field)
    lines = (DOCS / '03-stack.md').read_text(encoding='utf-8').splitlines()
    index = 0
    while index < len(lines):
        line = lines[index].strip()
        if not line:
            index += 1
            continue
        if line == '<!-- pagebreak -->':
            pass
        elif line.startswith('# '):
            doc.add_paragraph(line[2:], 'Title')
        elif line.startswith('## '):
            doc.add_heading(line[3:], 1)
        elif line.startswith('|'):
            rows = []
            while index < len(lines) and lines[index].strip().startswith('|'):
                values = [value.strip() for value in lines[index].strip().strip('|').split('|')]
                if not all(re.fullmatch(r'[- :]+', value) for value in values):
                    rows.append(values)
                index += 1
            add_table(doc, rows)
            table = doc.tables[-1]
            widths = WIDTHS[rows[0][0]]
            for column, width in zip(table.columns, widths):
                column.width = Cm(width)
            for row in table.rows:
                for cell, width in zip(row.cells, widths):
                    cell.width = Cm(width)
            continue
        else:
            add_inline(doc.add_paragraph(), line)
        index += 1
    doc.core_properties.title = 'Обоснование стека приложения Запомни'
    doc.core_properties.subject = 'Практика 3'
    doc.core_properties.author = ''
    doc.core_properties.last_modified_by = ''
    output = DOCS / '03-stack.docx'
    apply_document_style(doc)
    doc.save(output)
    check = Document(output)
    assert [len(table.rows) for table in check.tables] == [10, 11, 9, 11, 4]
    assert len([rel for rel in check.part.rels.values() if rel.reltype == RT.HYPERLINK]) == 28
    print(f'Created {output}; 5 tables, 8 comparison criteria, 10 risks, 28 official source links')


if __name__ == '__main__':
    build_document()
