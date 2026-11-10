"""Собирает векторные макеты практики 6 из матрицы состояний и токенов."""

from html import escape
import json
from pathlib import Path

from PIL import ImageFont

DOCS = Path(__file__).resolve().parents[1]
TOKENS = json.loads((DOCS / 'design/05-tokens.json').read_text(encoding='utf-8'))
MATRIX = json.loads((DOCS / 'design/06-screen-states.json').read_text(encoding='utf-8'))
OUT = DOCS / 'design/mockups'
WINDOWS_FONTS = Path('C:/Windows/Fonts')


def svg_attr(values):
    """Кодирует атрибуты XML."""
    return ' '.join(f'{key.replace("_", "-")}="{escape(str(value), quote=True)}"' for key, value in values.items())


def element(tag, **values):
    """Создаёт SVG-элемент без содержимого."""
    return f'<{tag} {svg_attr(values)}/>'


def group(name, x, y, children):
    """Группирует слои в локальных координатах."""
    return f'<g {svg_attr(dict(id=name, transform=f"translate({x} {y})"))}>' + ''.join(children) + '</g>'


def text(x, y, value, size, color, weight=400, anchor='start'):
    """Записывает строку как текстовый SVG-слой."""
    attrs = svg_attr(dict(x=x, y=y, font_family='Roboto, Arial, sans-serif', font_size=size, font_weight=weight, fill=color, text_anchor=anchor))
    return f'<text {attrs}>{escape(value)}</text>'


def wrap(value, width, size, weight):
    """Разбивает подпись по словам с запасом под метрики шрифта."""
    font = ImageFont.truetype(str(WINDOWS_FONTS / ('arialbd.ttf' if weight >= 600 else 'arial.ttf')), size)
    lines, current = [], ''
    for word in value.split():
        attempt = f'{current} {word}'.strip()
        if current and font.getlength(attempt) > width - 8:
            lines.append(current)
            current = word
        else:
            current = attempt
    return lines + [current] if current else lines


ICONS = {
    'back': 'M19 12H5m0 0 7-7m-7 7 7 7',
    'search': 'M17 17l4 4m-4-9a5 5 0 1 1-10 0 5 5 0 0 1 10 0',
    'catalog': 'M4 4h6v6H4zm10 0h6v6h-6zM4 14h6v6H4zm10 0h6v6h-6z',
    'decks': 'M7 7h13v13H7zM4 16V4h12',
    'settings': 'M4 6h16M4 12h16M4 18h16M9 4v4m6 2v4m-5 2v4',
    'loading': 'M20 9a8 8 0 1 0-1 8m1-13v5h-5',
    'empty': 'M4 8h16v11H4zm0 0 3-4h10l3 4M9 13h6',
    'offline': 'M3 9a14 14 0 0 1 18 0M6 13a9 9 0 0 1 12 0M9 17a4 4 0 0 1 6 0M3 3l18 18M12 21h.1',
    'error': 'M12 4l9 16H3zM12 10v4m0 3h.1',
    'check': 'm5 12 5 5 9-10',
    'gesture': 'M8 3h8a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zm2 14h4M3 8l2-2m14 12 2-2',
    'clock': 'M20 12a8 8 0 1 1-16 0 8 8 0 0 1 16 0zM12 7v5l3 2',
    'chevron': 'm9 5 7 7-7 7',
}


class Screen:
    """Собирает редактируемые слои одного макета."""

    def __init__(self, theme, title, width=360, height=820, tab=None, back=False, scale=1):
        self.theme = theme
        self.w, self.h = width, height
        self.scale = scale
        self.x, self.inner, self.y = 16, width - 32, 96
        self.limit = height - (98 if tab else 28)
        self.parts = [self.rect(0, 0, width, height, 'background', 24, 'border')]
        self.parts.extend(self.system_bar())
        self.parts.append(text(56 if back else 20, 71, title, 20, self.c('text'), 600))
        if back:
            self.parts.append(self.icon(18, 52, 'back'))
        self.parts.append(self.rect(16, 86, width - 32, 1, 'border'))
        if tab:
            self.parts.extend(self.nav(tab))
        self.parts.append(self.rect((width - 84) / 2, height - 14, 84, 4, 'outline', 2))

    def c(self, role):
        return TOKENS['colors'][role][self.theme == 'dark']

    def rect(self, x, y, w, h, role, radius=0, border=None):
        values = dict(x=x, y=y, width=w, height=h, rx=radius, fill=self.c(role))
        if border:
            values.update(stroke=self.c(border), stroke_width=1)
        return element('rect', **values)

    def icon(self, x, y, name, role='text'):
        return f'<g transform="translate({x} {y})">' + element('path', d=ICONS[name], stroke=self.c(role), stroke_width=1.8, stroke_linecap='round', stroke_linejoin='round', fill='none') + '</g>'

    def system_bar(self):
        color = self.c('text')
        return [text(24, 27, '9:41', 13, color, 600), text(self.w - 77, 27, 'LTE', 11, color, 600),
                element('rect', x=self.w - 46, y=17, width=20, height=10, rx=2, stroke=color, fill='none'),
                element('rect', x=self.w - 43, y=19, width=14, height=6, rx=1, fill=color),
                element('rect', x=self.w - 24, y=20, width=2, height=4, rx=1, fill=color)]

    def nav(self, active):
        y, w = self.h - 92, min(328, self.inner)
        x = (self.w - w) / 2
        parts = [self.rect(x, y, w, 68, 'surface', 16, 'border')]
        for i, (key, label) in enumerate([('catalog', 'Каталог'), ('decks', 'Мои наборы'), ('settings', 'Настройки')]):
            center = x + (i + 0.5) * w / 3
            role = 'onPrimarySoft' if key == active else 'textMuted'
            if key == active:
                parts.append(self.rect(center - 28, y + 7, 56, 30, 'primarySoft', 15))
            parts.extend([self.icon(center - 12, y + 10, key, role), text(center, y + 54, label, 12, self.c(role), 500, 'middle')])
        return parts

    def lines(self, content, x, y, width, style='body', role='text', scale=None):
        d = TOKENS['typography'][style]
        multiplier = self.scale if scale is None else scale
        size, line = int(d['size'] * multiplier), int(d['lineHeight'] * multiplier)
        weight = {'Regular': 400, 'Medium': 500, 'Bold': 700}[d['style']]
        rows = wrap(content, width, size, weight)
        for i, value in enumerate(rows):
            self.parts.append(text(x, y + size + i * line, value, size, self.c(role), weight))
        return line * len(rows)

    def paragraph(self, content, style='body', role='text', gap=12):
        self.y += self.lines(content, self.x, self.y, self.inner, style, role) + gap

    def pill(self, value, x, y, fg='onPrimarySoft', bg='primarySoft'):
        font = ImageFont.truetype(str(WINDOWS_FONTS / 'arial.ttf'), 12)
        w = round(font.getlength(value)) + 24
        self.parts += [self.rect(x, y, w, 24, bg, 12), text(x + w / 2, y + 16, value, 12, self.c(fg), 500, 'middle')]

    def action(self, label, mode='primary', x=None, w=None):
        x, w = self.x if x is None else x, self.inner if w is None else w
        colors = {'primary': ('primary', 'onPrimary'), 'secondary': ('surface', 'onPrimarySoft'), 'danger': ('dangerSoft', 'danger'), 'disabled': ('disabled', 'onDisabled')}
        bg, fg = colors[mode]
        size = int(14 * self.scale)
        row = int(20 * self.scale)
        labels = wrap(label, w - 36, size, 600)
        height = max(48, row * len(labels) + 24)
        self.parts.append(self.rect(x, self.y, w, height, bg, height // 2, 'outline' if mode == 'secondary' else None))
        for i, line in enumerate(labels):
            self.parts.append(text(x + w / 2, self.y + (height - row * len(labels)) / 2 + size + i * row, line, size, self.c(fg), 600, 'middle'))
        self.y += height + 8

    def field(self, label, value, error=None, search=False):
        self.y += self.lines(label, self.x, self.y, self.inner, 'label', 'danger' if error else 'text') + 4
        self.parts.append(self.rect(self.x, self.y, self.inner, 48, 'surface', 12, 'danger' if error else 'outline'))
        if search:
            self.parts.append(self.icon(self.x + 12, self.y + 12, 'search', 'textMuted'))
        self.parts.append(text(self.x + (44 if search else 12), self.y + 30, value, 16, self.c('text' if value and not value.startswith('Введите') else 'textMuted')))
        self.y += 54
        if error:
            self.y += self.lines(error, self.x, self.y, self.inner, 'caption', 'danger')
        self.y += 8

    def note(self, kind, title, detail, retry=False):
        colors = {'loading': ('surfaceRaised', 'text'), 'empty': ('surfaceRaised', 'text'), 'offline': ('warningSoft', 'warning'), 'error': ('dangerSoft', 'danger'), 'check': ('successSoft', 'success'), 'gesture': ('primarySoft', 'onPrimarySoft'), 'clock': ('surfaceRaised', 'text')}
        bg, fg = colors[kind]
        start = self.y
        background_index = len(self.parts)
        self.parts.append('')
        self.parts.append(self.icon(self.x + 14, self.y + 13, kind, fg))
        self.y += 12 + max(24, self.lines(title, self.x + 46, self.y + 12, self.inner - 60, 'label', fg))
        self.y += self.lines(detail, self.x + 14, self.y + 4, self.inner - 28, 'body', fg) + 14
        if retry:
            self.action('Повторить', 'secondary', self.x + 14, self.inner - 28)
            self.y += 4
        self.parts[background_index] = self.rect(self.x, start, self.inner, self.y - start, bg, 16)
        self.y += 12

    def hero(self, tag, title, detail):
        start = self.y
        index = len(self.parts)
        self.parts.append('')
        self.y += 16
        self.y += self.lines(tag, self.x + 16, self.y, self.inner - 32, 'caption', 'onPrimarySoft') + 6
        self.y += self.lines(title, self.x + 16, self.y, self.inner - 32, 'title', 'onPrimarySoft') + 4
        self.y += self.lines(detail, self.x + 16, self.y, self.inner - 32, 'body', 'onPrimarySoft') + 16
        self.parts[index] = self.rect(self.x, start, self.inner, self.y - start, 'primarySoft', 24)
        self.y += 12

    def deck(self, origin, title, info):
        start, index = self.y, len(self.parts)
        self.parts.append('')
        bg, fg = ('successSoft', 'success') if origin == 'Офлайн' else ('primarySoft', 'onPrimarySoft')
        self.pill(origin, self.x + 16, self.y + 14, fg, bg)
        self.y += 45
        self.y += self.lines(title, self.x + 16, self.y, self.inner - 52, 'heading') + 3
        self.y += self.lines(info, self.x + 16, self.y, self.inner - 40, 'caption', 'textMuted') + 15
        self.parts[index] = self.rect(self.x, start, self.inner, self.y - start, 'surface', 16, 'border')
        self.parts.append(self.icon(self.x + self.inner - 32, start + 49, 'chevron', 'textMuted'))
        self.y += 12

    def row(self, label, detail, control='arrow'):
        start, index = self.y, len(self.parts)
        self.parts.append('')
        self.y += 12
        self.y += self.lines(label, self.x + 14, self.y, self.inner - 82, 'label')
        self.y += self.lines(detail, self.x + 14, self.y, self.inner - 82, 'caption', 'textMuted') + 12
        self.parts[index] = self.rect(self.x, start, self.inner, self.y - start, 'surface', 12, 'border')
        if control in ('on', 'off'):
            x, y = self.x + self.inner - 62, (start + self.y) / 2 - 13
            self.parts.append(self.rect(x, y, 46, 26, 'primary' if control == 'on' else 'surfaceRaised', 13, None if control == 'on' else 'outline'))
            self.parts.append(element('circle', cx=x + (33 if control == 'on' else 13), cy=y + 13, r=8, fill=self.c('onPrimary' if control == 'on' else 'outline')))
        elif control == 'arrow':
            self.parts.append(self.icon(self.x + self.inner - 36, (start + self.y) / 2 - 12, 'chevron', 'textMuted'))
        self.y += 8

    def question(self, answer=False, gesture=True):
        start, index = self.y, len(self.parts)
        self.parts.append('')
        self.y += 16
        self.y += self.lines('ВОПРОС 03 / 12', self.x + 16, self.y, self.inner - 32, 'label', 'primary') + 10
        self.y += self.lines('Столица Португалии?', self.x + 16, self.y, self.inner - 32, 'title') + 14
        if answer:
            answer_y, answer_index = self.y, len(self.parts)
            self.parts.append('')
            self.y += 10
            self.y += self.lines('ОТВЕТ', self.x + 28, self.y, self.inner - 56, 'caption', 'onPrimarySoft') + 4
            self.y += self.lines('Лиссабон', self.x + 28, self.y, self.inner - 56, 'heading', 'onPrimarySoft') + 12
            self.parts[answer_index] = self.rect(self.x + 16, answer_y, self.inner - 32, self.y - answer_y, 'primarySoft', 12)
            self.y += 12
        else:
            hint = 'Откройте ответ кнопкой или переворотом телефона.' if gesture else 'Откройте ответ кнопкой.'
            self.y += self.lines(hint, self.x + 16, self.y, self.inner - 32, 'body', 'textMuted') + 14
        self.parts[index] = self.rect(self.x, start, self.inner, self.y - start, 'surface', 24, 'border')
        self.y += 12

    def overlay(self, title, detail, label='Удалить', mode='danger', time=False):
        # Затемнение и диалог остаются отдельными слоями.
        self.parts.append(element('rect', x=0, y=0, width=self.w, height=self.h, rx=24, fill=TOKENS['colors']['background'][1], fill_opacity=.64))
        old_x, old_y, old_inner = self.x, self.y, self.inner
        self.x, self.inner = (self.w - 308) / 2, 308
        self.y = int(self.h / 2 - (176 if time else 152))
        start, index = self.y, len(self.parts)
        self.parts.append('')
        self.x += 20
        self.inner -= 40
        self.y += 20
        self.paragraph(title, 'title', gap=8)
        self.paragraph(detail, role='textMuted')
        if time:
            self.parts.append(self.rect(self.x, self.y, self.inner, 78, 'primarySoft', 12))
            self.parts.append(text(self.x + self.inner / 2, self.y + 49, '20 : 00', 32, self.c('onPrimarySoft'), 700, 'middle'))
            self.y += 90
        self.action(label, mode)
        self.action('Отмена', 'secondary')
        self.parts[index] = self.rect(self.x - 20, start, 308, self.y - start + 12, 'surface', 24, 'border')
        self.x, self.y, self.inner = old_x, old_y, old_inner

    def output(self, name):
        assert self.y <= self.limit + 8, (name, self.y, self.limit)
        return self.parts


def catalog(s, state):
    """Показывает серверный каталог и поиск."""
    if state == 'data':
        s.hero('ЗАПОМНИ', 'Наборы для учёбы', 'Сохраняйте карточки и занимайтесь офлайн.')
    s.field('Поиск наборов', 'Планеты' if state == 'empty' else 'Название или предмет', search=True)
    if state == 'data':
        s.paragraph('Все наборы', 'heading', gap=8)
        s.deck('Каталог', 'Столицы Европы', '12 карточек · география')
        s.deck('Каталог', 'Английский A1', '24 карточки · иностранные языки')
    elif state == 'loading':
        s.note('loading', 'Загружаем каталог', 'Это может занять несколько секунд.')
    elif state == 'empty':
        s.note('empty', 'Ничего не найдено', 'Попробуйте другое название или очистите поиск.')
    elif state == 'error':
        s.note('error', 'Не удалось загрузить каталог', 'Проверьте соединение и повторите попытку.', retry=True)
    else:
        s.note('offline', 'Нет интернета', 'Скачанные наборы доступны в разделе «Мои наборы».', retry=True)
        s.action('Открыть мои наборы')


def decks(s, state):
    """Показывает локальный материал и удаление."""
    if state in ('data', 'delete'):
        s.hero('СЕГОДНЯ', '6 к повторению', 'Два набора доступны на этом устройстве.')
        s.action('Создать набор')
        s.deck('Офлайн', 'Столицы Европы', '12 карточек · 4 к повторению')
        s.deck('Свой набор', 'Термины биологии', '8 карточек · 2 к повторению')
        s.action('Статистика', 'secondary')
        if state == 'delete':
            s.overlay('Удалить набор?', 'Карточки и история повторений этого набора будут удалены.')
    elif state == 'loading':
        s.note('loading', 'Открываем мои наборы', 'Читаем сохранённый материал.')
    elif state == 'empty':
        s.note('empty', 'Наборов пока нет', 'Создайте свой набор или сохраните набор из каталога.')
        s.action('Создать набор')
        s.action('Перейти в каталог', 'secondary')
    else:
        s.note('error', 'Не удалось открыть наборы', 'Сохранённый материал не прочитался. Попробуйте снова.', retry=True)


def detail(s, state):
    """Показывает карточки и действия для источника набора."""
    if state == 'loading':
        s.note('loading', 'Открываем набор', 'Загружаем карточки.')
        return
    if state == 'error':
        s.note('error', 'Не удалось открыть набор', 'Проверьте соединение и попробуйте снова.', retry=True)
        return
    owned = state in ('owned', 'empty')
    s.hero('СВОЙ НАБОР' if owned else 'ГЕОГРАФИЯ', 'Термины биологии' if owned else 'Столицы Европы', '0 карточек' if state == 'empty' else '8 карточек' if owned else '12 карточек')
    if state == 'empty':
        s.note('empty', 'Карточек пока нет', 'Добавьте вопрос и ответ, чтобы начать занятие.')
        s.action('Добавить карточку')
        return
    if state == 'downloaded':
        s.note('check', 'Набор скачан', 'Можно заниматься без интернета.')
    s.paragraph('Карточки', 'heading', gap=8)
    cards = [('Митоз', 'Деление клетки'), ('Клетка', 'Единица строения живого')] if owned else [('Столица Португалии?', 'Лиссабон'), ('Столица Испании?', 'Мадрид')]
    for q, a in cards:
        s.row(q, a, 'none')
    if state == 'remote':
        s.action('Скачать для офлайна')
    elif state == 'downloading':
        s.note('loading', 'Скачиваем набор', 'Набор появится после загрузки всех карточек.')
        s.action('Скачиваем…', 'disabled')
    else:
        s.action('Начать занятие')
        s.action('Редактировать' if owned else 'Удалить скачивание', 'secondary')


def editor(s, state):
    """Показывает редактор материала и проверку полей."""
    if state in ('card', 'validation', 'delete'):
        s.paragraph('Редактирование карточки', 'heading')
        s.field('Вопрос', 'Введите вопрос' if state == 'validation' else 'Что такое митоз?', 'Введите вопрос' if state == 'validation' else None)
        s.field('Ответ', 'Деление клетки')
        s.paragraph('Изменение текста сбросит срок повторения карточки.', 'caption', 'textMuted')
        s.action('Сохранить карточку')
        s.action('Удалить карточку', 'danger')
        if state == 'delete':
            s.overlay('Удалить карточку?', 'Она исчезнет из набора и очереди повторения.')
        return
    fresh = state == 'new'
    s.paragraph('Новый набор' if fresh else 'Сведения о наборе', 'heading')
    s.field('Название', 'Введите название' if fresh else 'Термины биологии')
    s.field('Описание', 'Необязательно' if fresh else 'Основные понятия по биологии')
    if state == 'error':
        s.note('error', 'Изменения не сохранены', 'Введённый текст остался в форме.')
        s.action('Повторить сохранение')
    elif state == 'saving':
        s.note('loading', 'Сохраняем набор', 'Подождите завершения записи.')
        s.action('Сохраняем…', 'disabled')
    else:
        s.paragraph('Карточки', 'heading', gap=8)
        if fresh:
            s.paragraph('Добавьте карточки после сохранения набора.', 'body', 'textMuted')
        else:
            s.row('Что такое митоз?', 'Деление клетки')
            s.action('Добавить карточку', 'secondary')
        s.action('Сохранить набор')


def study(s, state):
    """Показывает вопрос, ответ и обработку оценки."""
    if state == 'noDue':
        s.note('clock', 'Пока нечего повторять', 'Следующая карточка будет доступна завтра в 14:30.')
        s.action('Вернуться к набору')
        return
    answer = state in ('answer', 'saving', 'error')
    s.question(answer, gesture=state != 'gestures')
    if state == 'gestures':
        s.note('gesture', 'Жесты недоступны', 'Продолжайте занятие кнопками.')
    if state == 'saving':
        s.note('loading', 'Сохраняем оценку', 'Следующая карточка откроется после записи.')
        s.action('Сохраняем…', 'disabled')
    elif state == 'error':
        s.note('error', 'Оценка не сохранена', 'Ответ останется открыт. Повторите сохранение.')
        s.action('Повторить сохранение')
    elif answer:
        s.action('Вспомнил')
        s.action('Нужно повторить', 'secondary')
    else:
        s.action('Показать ответ')
    s.action('Выйти из занятия', 'secondary')
    if state == 'exit':
        s.overlay('Выйти из занятия?', 'Сохранённые оценки останутся в истории. Текущая карточка без оценки не учитывается.', 'Выйти')


def results(s, state):
    """Показывает итог занятия и статистику."""
    if state == 'data':
        s.hero('ЗАНЯТИЕ ЗАВЕРШЕНО', '75,0% верных ответов', 'Столицы Европы · 12 оценённых карточек')
        s.row('Вспомнили', '9 из 12 карточек', 'none')
        s.row('Нужно повторить', '3 из 12 карточек', 'none')
        s.paragraph('Вся история', 'heading', gap=8)
        s.row('Сегодня', '12 карточек · 75,0%', 'none')
        s.row('Вчера', '8 карточек · 62,5%', 'none')
        s.action('К моим наборам')
    elif state == 'empty':
        s.note('empty', 'Нет данных', 'Оценки появятся после первого занятия.')
        s.action('К моим наборам')
    else:
        s.note('error', 'Не удалось прочитать историю', 'Повторите чтение локальной статистики.', retry=True)


def settings(s, state):
    """Показывает предпочтения и разрешение уведомлений."""
    s.paragraph('Внешний вид', 'heading', gap=8)
    s.row('Тема приложения', 'Как в системе · светлая · тёмная')
    s.paragraph('Управление', 'heading', gap=8)
    s.row('Жесты в занятии', 'Переворот и наклоны телефона', 'on')
    s.paragraph('Напоминания', 'heading', gap=8)
    s.row('Напоминать о повторении', 'При наличии карточек к повторению', 'off' if state in ('data', 'error') else 'on')
    if state in ('time', 'permission', 'saving'):
        s.row('Время напоминания', '20:00')
    if state == 'permission':
        s.note('error', 'Уведомления запрещены', 'Разрешите уведомления в настройках Android.')
        s.action('Открыть настройки Android', 'secondary')
    elif state == 'saving':
        s.note('loading', 'Сохраняем настройки', 'Изменения применятся после записи.')
    elif state == 'error':
        s.note('error', 'Настройки не сохранены', 'Повторите попытку.', retry=True)
    elif state == 'time':
        s.overlay('Время напоминания', 'Часы и минуты', 'Готово', 'primary', time=True)


BUILDERS = {'catalog': catalog, 'decks': decks, 'detail': detail, 'editor': editor, 'study': study, 'results': results, 'settings': settings}


def document(w, h, name, parts):
    """Формирует XML-документ без растровых изображений."""
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}"><title>{escape(name)}</title>' + ''.join(parts) + '</svg>\n'


def screen_for(screen, theme, state):
    """Возвращает макет для сочетания экрана, темы и состояния."""
    tab = screen['id'] if screen['id'] in ('catalog', 'decks', 'settings') else None
    title = {'detail': 'Набор', 'editor': 'Редактор', 'results': 'Статистика'}.get(screen['id'], screen['name'])
    phone = Screen(theme, title, tab=tab, back=tab is None)
    BUILDERS[screen['id']](phone, state['id'])
    return phone.output(f'{screen["id"]}/{theme}/{state["id"]}')


def board_for(screen, number):
    """Раскладывает варианты экранов по двум темам."""
    states = screen['states']
    w, h = 64 + len(states) * 360 + (len(states) - 1) * 24, 1940
    bg = TOKENS['colors']['surfaceRaised'][0]
    accent = TOKENS['colors']['primary'][0]
    ink = TOKENS['colors']['text'][0]
    muted = TOKENS['colors']['textMuted'][0]
    parts = [element('rect', width=w, height=h, rx=24, fill=bg), text(32, 42, f'ЗАПОМНИ / ПРАКТИКА 06 / {number:02}', 14, accent, 700), text(32, 91, screen['name'], 34, ink, 700), text(32, 125, screen['description'], 18, muted)]
    for row, theme in enumerate(MATRIX['themes']):
        top = 166 + row * 880
        parts.append(text(32, top, 'СВЕТЛАЯ ТЕМА' if theme == 'light' else 'ТЁМНАЯ ТЕМА', 13, accent, 700))
        for col, state in enumerate(states):
            x = 32 + col * 384
            parts.append(text(x, top + 28, state['name'], 16, ink, 600))
            parts.append(group(f'{screen["id"]}-{theme}-{state["id"]}', x, top + 42, screen_for(screen, theme, state)))
    return document(w, h, screen['name'], parts)


def adaptive(theme, kind):
    """Показывает планшет, альбомное занятие и крупный текст."""
    if kind == 'tablet':
        s = Screen(theme, 'Каталог', 800, 1280, tab='catalog')
        s.x, s.inner = 24, 336
        s.hero('КАТАЛОГ', 'Наборы для учёбы', 'Выберите набор в списке.')
        s.field('Поиск наборов', 'Название или предмет', search=True)
        s.deck('Каталог', 'Столицы Европы', '12 карточек · география')
        s.deck('Каталог', 'Английский A1', '24 карточки · языки')
        s.x, s.inner, s.y = 384, 392, 96
        s.hero('СОДЕРЖИМОЕ НАБОРА', 'Столицы Европы', '12 карточек · география')
        s.row('Столица Португалии?', 'Лиссабон', 'none')
        s.row('Столица Испании?', 'Мадрид', 'none')
        s.action('Скачать для офлайна')
        return s.output(kind)
    if kind == 'landscape':
        s = Screen(theme, 'Занятие', 800, 360, back=True)
        s.x, s.inner = 24, 360
        s.question(answer=True)
        s.x, s.inner, s.y = 408, 368, 96
        s.paragraph('Оцените ответ', 'heading', gap=8)
        s.action('Вспомнил')
        s.action('Нужно повторить', 'secondary')
        s.action('Выйти', 'secondary')
        return s.output(kind)
    s = Screen(theme, 'Занятие', back=True, scale=2)
    s.question(answer=True)
    s.action('Вспомнил')
    s.action('Нужно повторить', 'secondary')
    return s.output(kind)


def adaptive_board():
    """Собирает шесть примеров адаптации."""
    ink, accent = TOKENS['colors']['text'][0], TOKENS['colors']['primary'][0]
    w, h = 2100, 2810
    parts = [element('rect', width=w, height=h, rx=24, fill=TOKENS['colors']['surfaceRaised'][0]), text(32, 42, 'ЗАПОМНИ / ПРАКТИКА 06 / 08', 14, accent, 700), text(32, 91, 'Адаптация', 34, ink, 700)]
    for row, theme in enumerate(MATRIX['themes']):
        top = 138 + row * 1332
        parts.append(text(32, top, 'СВЕТЛАЯ ТЕМА' if theme == 'light' else 'ТЁМНАЯ ТЕМА', 13, accent, 700))
        for item, x in zip(MATRIX['adaptive'], [32, 856, 1680], strict=True):
            w_item, h_item = item['viewport']
            suffix = ' · 200%' if item['id'] == 'large-text' else ''
            parts.append(text(x, top + 25, f'{item["name"]} · {w_item} × {h_item}{suffix}', 16, ink, 600))
            parts.append(group(f'{item["id"]}-{theme}', x, top + 38, adaptive(theme, item['id'])))
    return document(w, h, 'Адаптация', parts)


def main():
    """Сохраняет векторные страницы, проверяя полноту матрицы и габариты."""
    OUT.mkdir(exist_ok=True)
    assert set(BUILDERS) == {screen['id'] for screen in MATRIX['screens']}
    count = 0
    for number, screen in enumerate(MATRIX['screens'], 1):
        (OUT / f'{number:02}-{screen["id"]}.svg').write_text(board_for(screen, number), encoding='utf-8')
        count += len(screen['states']) * len(MATRIX['themes'])
    (OUT / '08-adaptive.svg').write_text(adaptive_board(), encoding='utf-8')
    print(f'{count} phone variants and {len(MATRIX["adaptive"]) * len(MATRIX["themes"])} adaptive variants')


if __name__ == '__main__':
    main()
