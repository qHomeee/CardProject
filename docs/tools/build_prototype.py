"""Собирает автономный прототип из макетов и схемы переходов."""

import argparse
import json
from pathlib import Path
from xml.etree import ElementTree as ET

from build_mockups import BUILDERS, DOCS, MATRIX, Screen, document, screen_for

DESIGN = DOCS / 'design'
NAV = json.loads((DESIGN / '07-navigation.json').read_text(encoding='utf-8'))
W, H = MATRIX['phone']
TABS = ('Каталог', 'Мои наборы', 'Настройки')


def build_parts(key, route):
    """Подставляет данные одного демонстрационного занятия."""
    screen_id, state_id = route['view']
    screen = next(screen for screen in MATRIX['screens'] if screen['id'] == screen_id)
    state = next(state for state in screen['states'] if state['id'] == state_id)
    if route.get('dialog') or route.get('withoutDialog'):
        if route.get('dialog'):
            phone = Screen('light', 'Занятие', back=True)
            BUILDERS['study'](phone, state_id)
            phone.overlay(
                'Выйти из занятия?',
                'Сохранённые оценки останутся в истории. Текущая карточка без оценки не учитывается.',
                'Выйти',
            )
        else:
            phone = Screen('light', 'Настройки', tab='settings')
            BUILDERS['settings'](phone, 'reminder')
        parts = phone.output(key)
    else:
        parts = screen_for(screen, 'light', state)
    changes = {'ВОПРОС 03 / 12': 'ВОПРОС 12 / 12'}
    if key == 'repeat':
        changes.update({'75,0%': '66,7%', '9 из 12': '8 из 12', '3 из 12': '4 из 12'})
    if key == 'stats':
        changes.update({'ЗАНЯТИЕ ЗАВЕРШЕНО': 'ПОСЛЕДНЕЕ ЗАНЯТИЕ'})
    for old, new in changes.items():
        parts = [part.replace(old, new) for part in parts]
    return parts


def area(parts, label):
    """Находит границы кнопки или карточки по её видимой подписи."""
    if label == 'Назад':
        return [8, 39, 48, 48]
    if label in TABS:
        index = TABS.index(label)
        left = 16 + round(index * 328 / 3)
        right = 16 + round((index + 1) * 328 / 3)
        return [left, H - 92, right - left, 68]
    elements = [ET.fromstring(part) for part in parts]
    for index, text in enumerate(elements):
        if text.tag != 'text' or text.text != label:
            continue
        x, y = float(text.attrib['x']), float(text.attrib['y'])
        for rect in reversed(elements[:index]):
            if rect.tag != 'rect' or 'x' not in rect.attrib:
                continue
            left, top, width, height = (
                float(rect.attrib[name]) for name in ('x', 'y', 'width', 'height')
            )
            if 100 <= width < W and 48 <= height < 220 and left <= x <= left + width and top < y < top + height:
                return [round(value) for value in (left, top, width, height)]
    raise ValueError(f'Подпись или зона нажатия не найдена: {label}')


def assemble():
    """Проверяет связи и привязывает их к элементам макетов."""
    result = {}
    for key, route in NAV['screens'].items():
        parts = build_parts(key, route)
        links = []
        for label, target in route['links'].items():
            if target not in NAV['screens']:
                raise ValueError(f'{key}: отсутствует экран {target}')
            try:
                bounds = area(parts, label)
            except ValueError as error:
                raise ValueError(f'{key}: {error}') from error
            links.append({'label': label, 'target': target, 'bounds': bounds})
        result[key] = {
            'title': route['title'],
            'svg': document(W, H, route['title'], parts),
            'links': links,
        }

    pending = list(NAV['starts'])
    reached = set()
    while pending:
        key = pending.pop()
        if key not in reached:
            reached.add(key)
            pending.extend(link['target'] for link in result[key]['links'])
    if set(result) != reached:
        raise ValueError(f'Недостижимые экраны: {set(result) - reached}')
    return result


HTML = '''<!doctype html>
<html lang="ru">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Запомни — прототип</title>
<style>
  * { box-sizing: border-box; }
  body {
    margin: 0; min-height: 100dvh; padding: 16px;
    background: #e9edf4; color: #171e2a;
    font: 15px/1.4 Roboto, Arial, sans-serif;
  }
  main { width: min(100%, 360px); margin: auto; }
  nav { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 10px; }
  nav button {
    font: inherit; cursor: pointer; border: 1px solid #63738a;
    border-radius: 100px; padding: 6px 12px; background: #fff; color: #171e2a;
  }
  #screen { position: relative; aspect-ratio: 360 / 820; }
  #screen svg { display: block; width: 100%; height: 100%; }
  #screen button {
    position: absolute; padding: 0; border: 0; border-radius: 10px;
    background: transparent; cursor: pointer;
  }
  button:focus-visible { outline: 3px solid #2456b8; outline-offset: -3px; }
</style>
</head>
<body>
<main aria-label="Прототип приложения Запомни">
  <nav aria-label="Начало сценария" id="starts"></nav>
  <div id="screen" role="group" tabindex="-1"></div>
</main>
<script>
  const starts = STARTS_JSON;
  const frames = FRAMES_JSON;
  const screen = document.querySelector('#screen');

  function show(key, focus) {
    const frame = frames[key];
    screen.innerHTML = frame.svg;
    screen.dataset.route = key;
    screen.setAttribute('aria-label', frame.title);
    screen.querySelector('svg').setAttribute('aria-hidden', 'true');
    for (const {label, target, bounds} of frame.links) {
      const button = document.createElement('button');
      const [x, y, width, height] = bounds;
      button.type = 'button';
      button.setAttribute('aria-label', label);
      button.style.left = (100 * x / 360) + '%';
      button.style.top = (100 * y / 820) + '%';
      button.style.width = (100 * width / 360) + '%';
      button.style.height = (100 * height / 820) + '%';
      button.addEventListener('click', () => show(target, true));
      screen.append(button);
    }
    if (focus) screen.focus();
  }

  for (const [key, label] of Object.entries(starts)) {
    const button = document.createElement('button');
    button.type = 'button';
    button.textContent = label;
    button.addEventListener('click', () => show(key, true));
    document.querySelector('#starts').append(button);
  }
  show('catalog', false);
</script>
</body>
</html>
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--figma-assets', type=Path, help='Экспортировать отдельные макеты SVG и координаты переходов')
    args = parser.parse_args()
    frames = assemble()
    output = HTML.replace('STARTS_JSON', json.dumps(NAV['starts'], ensure_ascii=False))
    output = output.replace('FRAMES_JSON', json.dumps(frames, ensure_ascii=False).replace('</', '<\\/'))
    (DESIGN / '07-prototype.html').write_text(output, encoding='utf-8')
    if args.figma_assets:
        args.figma_assets.mkdir(parents=True, exist_ok=True)
        for key, frame in frames.items():
            (args.figma_assets / f'{key}.svg').write_text(frame['svg'], encoding='utf-8')
        links = {key: {'title': frame['title'], 'links': frame['links']} for key, frame in frames.items()}
        (args.figma_assets / 'links.json').write_text(
            json.dumps(links, ensure_ascii=False, indent=2) + '\n', encoding='utf-8',
        )
    print(f'Прототип: {len(frames)} экранов, {sum(len(frame["links"]) for frame in frames.values())} переходов.')


if __name__ == '__main__':
    main()
