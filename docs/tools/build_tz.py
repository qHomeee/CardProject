"""Собирает ТЗ из Markdown и создаёт use-case-диаграмму для документа.

Запуск из любой папки: python docs/tools/build_tz.py.
Зависимости: python-docx и Pillow. Рендеринг DOCX выполняется отдельно.
"""

from pathlib import Path
import re
from html import escape

from docx import Document
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.section import WD_ORIENT, WD_SECTION_START
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.opc.constants import RELATIONSHIP_TYPE as RT
from docx.shared import Cm, Pt, RGBColor
from PIL import Image, ImageDraw, ImageFont
from document_style import apply_document_style, FONT_NAME

DOCS = Path(__file__).resolve().parents[1]


def build_diagram():
    """Создаёт SVG и PNG из одинаковых координат и подписей UML-диаграммы."""
    target = DOCS / 'diagrams'
    target.mkdir(exist_ok=True)
    canvas = Image.new('RGB', (1800, 1260), 'white')
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.truetype('C:/Windows/Fonts/times.ttf', 37)
    svg = ['<svg xmlns="http://www.w3.org/2000/svg" width="1800" height="1260" viewBox="0 0 1800 1260">',
           '<rect width="1800" height="1260" fill="white"/>']

    def line(points, color='#303030', width=3):
        draw.line(points, fill=color, width=width)
        svg.append('<polyline points="' + ' '.join(f'{x},{y}' for x, y in points) + f'" fill="none" stroke="{color}" stroke-width="{width}"/>')

    def label(center, text):
        x, y = center
        draw.multiline_text((x, y), text, font=font, fill='black', anchor='mm', align='center', spacing=5)
        lines = text.split('\n')
        for index, part in enumerate(lines):
            baseline = y - (len(lines) - 1) * 21 + index * 42 + 12
            svg.append(f'<text x="{x}" y="{baseline}" font-family="Times New Roman, serif" font-size="37" text-anchor="middle" fill="black">{escape(part)}</text>')

    def actor(x, y, text):
        draw.ellipse((x - 28, y - 28, x + 28, y + 28), outline='#303030', width=3)
        svg.append(f'<circle cx="{x}" cy="{y}" r="28" fill="white" stroke="#303030" stroke-width="3"/>')
        line([(x, y + 28), (x, y + 125)])
        line([(x - 60, y + 70), (x + 60, y + 70)])
        line([(x - 50, y + 200), (x, y + 125), (x + 50, y + 200)])
        label((x, y + 255), text)

    draw.rectangle((340, 55, 1460, 1215), outline='#303030', width=3)
    svg.append('<rect x="340" y="55" width="1120" height="1160" fill="none" stroke="#303030" stroke-width="3"/>')
    label((900, 102), 'Система Запомни')
    actor(140, 500, 'Учащийся')
    actor(1640, 900, 'Редактор\nкаталога')
    cases = [
        'Просматривать и искать\nобщий каталог',
        'Скачивать набор',
        'Управлять собственными\nнаборами и карточками',
        'Заниматься по карточкам\nс жестами или кнопками',
        'Просматривать результаты\nи статистику',
        'Настраивать тему\nжесты и напоминания',
        'Изменять общий каталог\nпри доступе редактора',
    ]
    for index, text in enumerate(cases):
        y = 200 + index * 145
        if index < 6:
            line([(200, 570), (570, y)])
        else:
            line([(1580, 970), (1230, y)])
        draw.ellipse((570, y - 59, 1230, y + 59), fill='white', outline='#303030', width=3)
        svg.append(f'<ellipse cx="900" cy="{y}" rx="330" ry="59" fill="white" stroke="#303030" stroke-width="3"/>')
        label((900, y), text)
    svg.append('</svg>')
    (target / '02-use-cases.svg').write_text('\n'.join(svg), encoding='utf-8')
    canvas.save(target / '02-use-cases.png')


def add_inline(paragraph, text):
    """Переносит выделение и настоящие внешние гиперссылки в Word."""
    for part in re.split(r'(\[[^\]]+\]\(https?://[^)]+\)|\*\*[^*]+\*\*)', text):
        match = re.fullmatch(r'\[([^\]]+)\]\((https?://[^)]+)\)', part)
        if match:
            link = OxmlElement('w:hyperlink')
            link.set(qn('r:id'), paragraph.part.relate_to(match[2], RT.HYPERLINK, is_external=True))
            run = OxmlElement('w:r')
            props = OxmlElement('w:rPr')
            color = OxmlElement('w:color'); color.set(qn('w:val'), '000000'); props.append(color)
            run.append(props)
            value = OxmlElement('w:t'); value.text = match[1]; run.append(value)
            link.append(run); paragraph._p.append(link)
        else:
            run = paragraph.add_run(part.removeprefix('**').removesuffix('**'))
            run.bold = part.startswith('**')


def add_table(doc, rows):
    """Вставляет таблицу с повторяемой шапкой и расширяемыми строками."""
    table = doc.add_table(rows=0, cols=len(rows[0]))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    widths = [3.6, 6.9, 6.5] if len(rows[0]) == 3 else [4.6, 12.4]
    if len(rows[0]) == 4:
        widths = [3.9, 4.4, 4.35, 4.35]
    if rows[0][0] == 'ID':
        widths = [1.4, 11.9, 3.7]
    if rows[0][0] == 'Оценка':
        widths = [7.3, 2.4, 7.3]
    for column, width in zip(table.columns, widths):
        column.width = Cm(width)
    borders = OxmlElement('w:tblBorders')
    for side in ['top', 'left', 'bottom', 'right', 'insideH', 'insideV']:
        edge = OxmlElement('w:' + side)
        for key, value in [('val', 'single'), ('sz', '6'), ('color', '000000')]:
            edge.set(qn('w:' + key), value)
        borders.append(edge)
    table._tbl.tblPr.append(borders)
    for index, values in enumerate(rows):
        row = table.add_row()
        tr_pr = row._tr.get_or_add_trPr()
        tr_pr.append(OxmlElement('w:cantSplit'))
        if index == 0:
            tr_pr.append(OxmlElement('w:tblHeader'))
        for cell, value, width in zip(row.cells, values, widths):
            cell.width = Cm(width)
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            props = cell._tc.get_or_add_tcPr()
            shade = OxmlElement('w:shd')
            shade.set(qn('w:fill'), 'FFFFFF')
            props.append(shade)
            margins = OxmlElement('w:tcMar')
            for side in ['top', 'left', 'bottom', 'right']:
                margin = OxmlElement('w:' + side)
                margin.set(qn('w:w'), '75'); margin.set(qn('w:type'), 'dxa'); margins.append(margin)
            props.append(margins)
            paragraph = cell.paragraphs[0]
            paragraph.paragraph_format.space_after = Pt(2)
            paragraph.paragraph_format.line_spacing = 1.0
            add_inline(paragraph, value)
            for run in paragraph.runs:
                run.font.size = Pt(14)
                if index == 0:
                    run.bold = True
    doc.add_paragraph().paragraph_format.space_after = Pt(1)


def build_document():
    """Собирает документ с явными разделами и доступными подписями рисунка."""
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
    run = footer.add_run('Запомни    Техническое задание    ')
    run.font.size = Pt(14)
    field = OxmlElement('w:fldSimple'); field.set(qn('w:instr'), 'PAGE'); footer._p.append(field)
    lines = (DOCS / '02-tz.md').read_text(encoding='utf-8').splitlines()
    index = 0
    while index < len(lines):
        line = lines[index].strip()
        if not line:
            index += 1; continue
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
            continue
        elif line.startswith('!['):
            match = re.fullmatch(r'!\[([^\]]+)\]\(([^)]+)\)', line)
            diagram_section = doc.add_section(WD_SECTION_START.NEW_PAGE)
            diagram_section.orientation = WD_ORIENT.LANDSCAPE
            diagram_section.page_width, diagram_section.page_height = Cm(29.7), Cm(21)
            diagram_section.top_margin = diagram_section.bottom_margin = Cm(1.4)
            diagram_section.left_margin = diagram_section.right_margin = Cm(2)
            paragraph = doc.add_paragraph()
            paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
            # При ширине 24 см подписи 37 px на исходнике 1800 px дают 14 pt.
            shape = paragraph.add_run().add_picture(str(DOCS / match[2]), width=Cm(24))
            shape._inline.docPr.set('descr', match[1])
            content_section = doc.add_section(WD_SECTION_START.NEW_PAGE)
            content_section.orientation = WD_ORIENT.PORTRAIT
            content_section.page_width, content_section.page_height = Cm(21), Cm(29.7)
            content_section.top_margin = content_section.bottom_margin = Cm(1.6)
            content_section.left_margin = content_section.right_margin = Cm(2)
        else:
            add_inline(doc.add_paragraph(), line)
        index += 1
    doc.core_properties.title = 'Техническое задание приложения Запомни'
    doc.core_properties.subject = 'Практика 2'
    doc.core_properties.author = ''
    doc.core_properties.last_modified_by = ''
    output = DOCS / '02-tz.docx'
    apply_document_style(doc)
    doc.save(output)
    check = Document(output)
    assert len(check.tables) == 4
    assert len(check.inline_shapes) == 1
    assert len([r for r in check.part.rels.values() if r.reltype == RT.HYPERLINK]) == 3
    all_text = '\n'.join(p.text for p in check.paragraphs)
    assert all(f'FR{i:02d}' in all_text for i in range(1, 17))
    assert all(f'A{i:02d}' in all_text for i in range(1, 17))
    print(f'Created {output}; 16 requirements and acceptance criteria, 4 tables, 1 diagram, 3 source links')


if __name__ == '__main__':
    build_diagram()
    build_document()
