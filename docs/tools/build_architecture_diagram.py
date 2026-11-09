"""Собирает SVG и PNG диаграммы зависимостей практики 4."""

from html import escape
from math import hypot
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

DOCS = Path(__file__).resolve().parents[1]
WIDTH, HEIGHT = 1440, 658
LABEL_SIZE = 42
INSERT_WIDTH_CM = 16.93
NODES = [
    ((439, 12, 1001, 136), ('DI и Koin', 'Связывание классов')),
    ((24, 190, 664, 348), ('presentation', 'Compose и ViewModel', 'UiState и навигация')),
    ((776, 190, 1416, 348), ('data', 'Room, DataStore и Ktor', 'Датчики и напоминания')),
    ((321, 458, 1119, 646), (':domain на Kotlin/JVM', 'Модели и интерфейсы', 'Use cases и правила повторения')),
]
EDGES = [
    ((542, 136), (418, 190), True),
    ((898, 136), (1022, 190), True),
    ((720, 136), (720, 458), True),
    ((395, 348), (531, 458), False),
    ((1045, 348), (909, 458), False),
]


def build_diagram():
    """Рисует общие узлы и стрелки для Markdown и Word."""
    canvas = Image.new('RGB', (WIDTH, HEIGHT), 'white')
    draw = ImageDraw.Draw(canvas)
    regular = ImageFont.truetype('times.ttf', LABEL_SIZE)
    bold = ImageFont.truetype('timesbd.ttf', LABEL_SIZE)
    svg = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{WIDTH}" height="{HEIGHT}" viewBox="0 0 {WIDTH} {HEIGHT}" role="img">',
        '<title>Зависимости слоёв приложения Запомни</title>',
        '<desc>Presentation и data зависят от domain. DI связывает реализации трёх слоёв.</desc>',
        '<rect width="100%" height="100%" fill="white"/>',
    ]

    for start, end, dashed in EDGES:
        x, y = start
        dx, dy = end[0] - x, end[1] - y
        length = hypot(dx, dy)
        ux, uy = dx / length, dy / length
        base = (end[0] - 19 * ux, end[1] - 19 * uy)
        if dashed:
            for offset in range(0, int(length - 15), 19):
                stop = min(offset + 12, length - 15)
                draw.line((x + ux * offset, y + uy * offset, x + ux * stop, y + uy * stop), fill='black', width=4)
        else:
            draw.line((start, base), fill='black', width=4)
        dash = ' stroke-dasharray="12 7"' if dashed else ''
        svg.append(f'<line x1="{x}" y1="{y}" x2="{base[0]:.1f}" y2="{base[1]:.1f}" stroke="black" stroke-width="4"{dash}/>')
        head = [end, (base[0] - 9 * uy, base[1] + 9 * ux), (base[0] + 9 * uy, base[1] - 9 * ux)]
        draw.polygon(head, fill='black')
        points = ' '.join(f'{px:.1f},{py:.1f}' for px, py in head)
        svg.append(f'<polygon points="{points}" fill="black"/>')

    svg.append(f'<g font-family="Times New Roman, serif" font-size="{LABEL_SIZE}" text-anchor="middle" fill="black">')
    for (left, top, right, bottom), lines in NODES:
        draw.rectangle((left, top, right, bottom), fill='white', outline='black', width=4)
        svg.append(f'<rect x="{left}" y="{top}" width="{right - left}" height="{bottom - top}" fill="white" stroke="black" stroke-width="4"/>')
        x = (left + right) // 2
        baseline = (top + bottom) // 2 - (len(lines) - 1) * 23 + 13
        for index, line in enumerate(lines):
            font = bold if index == 0 else regular
            weight = ' font-weight="bold"' if index == 0 else ''
            draw.text((x, baseline), line, fill='black', font=font, anchor='ms')
            svg.append(f'<text x="{x}" y="{baseline}"{weight}>{escape(line)}</text>')
            baseline += 46
    svg.extend(['</g>', '</svg>'])

    target = DOCS / 'diagrams'
    target.mkdir(exist_ok=True)
    (target / '04-dependencies.svg').write_text('\n'.join(svg) + '\n', encoding='utf-8')
    canvas.save(target / '04-dependencies.png')


if __name__ == '__main__':
    build_diagram()
