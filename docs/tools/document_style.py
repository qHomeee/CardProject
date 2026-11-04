"""Единое оформление DOCX по требованиям пользователя.

Times New Roman 14 pt применяется к основному тексту, заголовкам, таблицам,
гиперссылкам и колонтитулам. Таблицы имеют белый фон и чёрные границы.
Межстрочный интервал 1,5. Первая строка основного текста отступает на 1,25 см.
"""

import re

from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm
from docx.text.paragraph import Paragraph

FONT_NAME = 'Times New Roman'
FONT_HALF_POINTS = '28'


def _child(parent, name):
    """Возвращает или создаёт непосредственный элемент свойств Word."""
    child = parent.find(qn(name))
    if child is None:
        child = OxmlElement(name)
        parent.append(child)
    return child


def _format_run_properties(properties):
    """Задаёт явный шрифт и размер, включая кириллицу и сложные письменности."""
    fonts = _child(properties, 'w:rFonts')
    fonts.attrib.clear()
    for name in ['ascii', 'hAnsi', 'eastAsia', 'cs']:
        fonts.set(qn('w:' + name), FONT_NAME)
    for name in ['w:sz', 'w:szCs']:
        _child(properties, name).set(qn('w:val'), FONT_HALF_POINTS)
    color = _child(properties, 'w:color')
    color.attrib.clear()
    color.set(qn('w:val'), '000000')


def apply_paragraph_layout(doc):
    """Выставляет интервал и отличает абзацы текста от таблиц и заголовков."""
    body_indent = Cm(1.25)
    for style_id in ('Normal', 'Body Text'):
        paragraph_format = doc.styles[style_id].paragraph_format
        paragraph_format.line_spacing = 1.5
        paragraph_format.first_line_indent = body_indent

    for part in doc.part.package.parts:
        root = getattr(part, '_element', None)
        if root is None:
            continue
        for element in root.iter(qn('w:p')):
            text = ''.join(node.text or '' for node in element.iter(qn('w:t'))).strip()
            properties = element.pPr
            style = properties.pStyle.val if properties is not None and properties.pStyle is not None else 'Normal'
            is_list = re.match(r'^(?:\d+[.)]|[-–•])\s', text) or (properties is not None and properties.numPr is not None)
            is_body = (
                element.getparent().tag == qn('w:body')
                and style in ('Normal', 'BodyText')
                and text
                and not is_list
            )
            paragraph_format = Paragraph(element, part).paragraph_format
            paragraph_format.line_spacing = 1.5
            paragraph_format.first_line_indent = body_indent if is_body else Cm(0)


def apply_document_style(doc):
    """Применяет единый стиль без изменения текста и структуры ссылок."""
    defaults = _child(doc.styles.element, 'w:docDefaults')
    run_defaults = _child(defaults, 'w:rPrDefault')
    _format_run_properties(_child(run_defaults, 'w:rPr'))

    for style in doc.styles:
        _format_run_properties(style.element.get_or_add_rPr())
    for name in ['Title', 'Heading 1', 'Heading 2', 'Heading 3']:
        doc.styles[name].font.bold = True
    for border in list(doc.styles.element.iter(qn('w:pBdr'))):
        border.getparent().remove(border)

    for part in doc.part.package.parts:
        root = getattr(part, '_element', None)
        if root is None:
            continue
        for run in root.iter(qn('w:r')):
            properties = run.find(qn('w:rPr'))
            if properties is None:
                properties = OxmlElement('w:rPr')
                run.insert(0, properties)
            _format_run_properties(properties)
        for properties in list(root.iter(qn('w:rPr'))):
            _format_run_properties(properties)
        for border in list(root.iter(qn('w:pBdr'))):
            border.getparent().remove(border)

    for table in doc.tables:
        props = table._tbl.tblPr
        table_style = props.find(qn('w:tblStyle'))
        if table_style is not None:
            props.remove(table_style)
        borders = _child(props, 'w:tblBorders')
        for edge in list(borders):
            borders.remove(edge)
        for side in ['top', 'left', 'bottom', 'right', 'insideH', 'insideV']:
            edge = OxmlElement('w:' + side)
            edge.set(qn('w:val'), 'single')
            edge.set(qn('w:sz'), '6')
            edge.set(qn('w:color'), '000000')
            borders.append(edge)
        for index, row in enumerate(table.rows):
            row_props = row._tr.get_or_add_trPr()
            _child(row_props, 'w:cantSplit')
            if index == 0:
                _child(row_props, 'w:tblHeader')
            for cell in row.cells:
                cell_props = cell._tc.get_or_add_tcPr()
                shade = _child(cell_props, 'w:shd')
                shade.attrib.clear()
                shade.set(qn('w:val'), 'clear')
                shade.set(qn('w:fill'), 'FFFFFF')
                for paragraph in cell.paragraphs:
                    for run in paragraph.runs:
                        if index == 0:
                            run.bold = True

    apply_paragraph_layout(doc)
