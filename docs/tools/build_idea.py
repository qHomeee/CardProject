"""Собирает карточку идеи из разделов README с единым оформлением DOCX."""

from pathlib import Path
import re

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt

from build_tz import add_inline, add_table
from document_style import apply_document_style

ROOT = Path(__file__).resolve().parents[2]


def build_document():
    """Сохраняет текст карточки и ссылки с Times New Roman 14 pt."""
    doc = Document()
    section = doc.sections[0]
    section.page_width, section.page_height = Cm(21), Cm(29.7)
    section.top_margin = section.bottom_margin = Cm(1.8)
    section.left_margin = section.right_margin = Cm(2)
    doc.styles['Normal'].paragraph_format.space_after = Pt(6)
    for name in ['Heading 1', 'Heading 2']:
        doc.styles[name].paragraph_format.keep_with_next = True
        doc.styles[name].paragraph_format.space_before = Pt(10)
        doc.styles[name].paragraph_format.space_after = Pt(6)
    doc.add_paragraph('Карточка идеи приложения Запомни', 'Title')
    doc.add_paragraph('Практика 1    4 ноября 2026 года', 'Subtitle')
    doc.add_paragraph('Тема согласована с преподавателем. Предлагается приложение для повторения учебного материала по карточкам с локальным прогрессом и управлением движением телефона. Документ фиксирует задачу, границы первой версии и основания для дальнейшего ТЗ.')
    footer = section.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    footer.add_run('Запомни    Карточка идеи    ')
    field = OxmlElement('w:fldSimple')
    field.set(qn('w:instr'), 'PAGE')
    footer._p.append(field)
    lines = (ROOT / 'README.md').read_text(encoding='utf-8').splitlines()
    index = lines.index('## Проблема и аудитория')
    stop = lines.index('## Текущее состояние и файлы')
    while index < stop:
        line = lines[index].strip()
        if not line:
            index += 1
            continue
        if line.startswith('## '):
            doc.add_heading(line[3:], 1)
        elif line.startswith('|'):
            rows = []
            while index < stop and lines[index].strip().startswith('|'):
                values = [value.strip() for value in lines[index].strip().strip('|').split('|')]
                if not all(re.fullmatch(r'[- :]+', value) for value in values):
                    rows.append(values)
                index += 1
            add_table(doc, rows)
            continue
        else:
            add_inline(doc.add_paragraph(), line)
        index += 1
    doc.core_properties.title = 'Карточка идеи приложения Запомни'
    doc.core_properties.subject = 'Практика 1'
    doc.core_properties.author = ''
    doc.core_properties.last_modified_by = ''
    apply_document_style(doc)
    output = ROOT / 'docs/01-idea.docx'
    doc.save(output)
    check = Document(output)
    assert len(check.tables) == 1 and len(check.tables[0].rows) == 7
    print(f'Created {output}; 3 analogues, 6 criteria')


if __name__ == '__main__':
    build_document()
