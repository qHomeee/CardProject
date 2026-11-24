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

Модульные тесты проверяют валидацию, редакторский доступ и ответы об ошибках. Их можно запустить из папки `backend` с установленными зависимостями.

```powershell
uv run --locked python -m pytest -q tests/unit
```

Для полного прогона создайте отдельную пустую базу с ролью, которая может создавать схемы. Из папки `backend` выполните следующие команды.

```powershell
$env:TEST_DATABASE_URL = 'postgresql+psycopg://catalog:DB_PASSWORD@127.0.0.1:5432/zapomni_test'
uv run --locked python -m pytest -q
```

Для каждого интеграционного теста создаётся и мигрируется схема с отдельным именем. После теста удаляется только эта схема. Команда `uv run --locked alembic check` проверяет соответствие моделей и миграций в базе из DATABASE_URL. Откат начальной миграции удаляет таблицы и данные каталога.

## Развёртывание на VPS

Текущий экземпляр запущен на Ubuntu 24.04. Nginx принимает HTTPS на 443 и пересылает запросы в Uvicorn на `127.0.0.1:8000`. FastAPI подключается к PostgreSQL через локальный Unix-сокет под ролью `zapomni`. Редакторский ключ хранится в файле `/etc/zapomni/catalog.env` с правами `600`.

- [Каталог](https://153.80.185.223/api/v1/decks)
- [Swagger UI](https://153.80.185.223/docs)
- [OpenAPI JSON](https://153.80.185.223/openapi.json)

На чистой Ubuntu установите пакеты `nginx postgresql python3-venv ca-certificates curl`. Создайте окружение командой `python3 -m venv /opt/zapomni/tooling` и установите в него `uv<1` и `certbot>=5.4,<6` через pip. С переменной `UV_PYTHON_INSTALL_DIR=/opt/zapomni/python` установите Python 3.13 командой `/opt/zapomni/tooling/bin/uv python install 3.13 --no-bin`.

Упакуйте каталог `backend`, перенесите архив по SSH и распакуйте его в отдельную папку `/opt/zapomni/releases/<коммит>`. Из этой папки выполните с правами администратора:

```bash
bash deploy/activate.sh
bash deploy/enable-https.sh PUBLIC_IPV4
```

`activate.sh` устанавливает зависимости по lock-файлу, применяет Alembic и переключает службу на релиз после установки. `enable-https.sh` выпускает сертификат на переданный IP и включает таймер продления. Порты 80 и 443 должны быть доступны извне. Состояние службы и продления проверяется командами `systemctl status zapomni` и `systemctl list-timers zapomni-certbot.timer`. Для HTTPS-запросов Nginx ограничивает тело 64 КиБ.

## Коллекция Postman

Импортируйте `catalog.postman_collection.json`. Для чтения с VPS установите `base_url` в `https://153.80.185.223`. Поиск показывает ID наборов для переменной `deck_id`. Ответ набора содержит ID карточек для `card_id`.

Для папки «Создание, изменение и удаление» используйте локальный сервер с тестовой базой и задайте `editor_key` в приватном окружении Postman. Запросы создания автоматически сохраняют ID. Папка «Ошибки» показывает отказ без ключа и проверку пустого названия.
