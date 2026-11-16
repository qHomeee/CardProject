# Сервер каталога Запомни

FastAPI предоставляет общий каталог наборов. Для запуска нужны Python 3.13, [uv](https://docs.astral.sh/uv/getting-started/installation/) и пустая база PostgreSQL. Роль в примере должна владеть базой и иметь право создавать таблицы. Используйте отдельную тестовую базу для проверок.

## Запуск в PowerShell

В терминале из корня проекта выполните команды. Замените имя роли, базы и пароль в DATABASE_URL на свои значения. Если пароль содержит специальные символы URL, закодируйте их.

```powershell
Set-Location backend
uv sync --locked
$env:DATABASE_URL = 'postgresql+psycopg://catalog:DB_PASSWORD@127.0.0.1:5432/zapomni'
$env:EDITOR_API_KEY = (uv run --locked python -c 'import secrets; print(secrets.token_urlsafe(32))').Trim()
uv run --locked alembic upgrade head
uv run --locked uvicorn catalog.main:app --host 127.0.0.1 --port 8000
```

[Swagger UI](http://127.0.0.1:8000/docs) показывает операции и примеры из [контракта OpenAPI](openapi.yaml). Для запросов записи в Swagger укажите значение EDITOR_API_KEY через Authorize. Приватный ключ вводится только редактором. Настройки действуют для текущего терминала и запущенного из него сервера. Если ключ опущен, чтение доступно, а запись отвечает 503.

## Проверка

Создайте отдельную пустую базу с ролью, которая может создавать схемы. Из папки `backend` выполните следующие команды.

```powershell
$env:TEST_DATABASE_URL = 'postgresql+psycopg://catalog:DB_PASSWORD@127.0.0.1:5432/zapomni_test'
uv run --locked python -m pytest -q
```

Для каждого интеграционного теста создаётся и мигрируется схема с отдельным именем. После теста удаляется только эта схема. Команда `uv run --locked alembic check` проверяет соответствие моделей и миграций в базе из DATABASE_URL. Откат начальной миграции удаляет таблицы и данные каталога.
