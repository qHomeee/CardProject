#!/usr/bin/env bash
set -euo pipefail

release="$(cd "$(dirname "$0")/.." && pwd -P)"
case "$release" in
    /opt/zapomni/releases/*) ;;
    *) echo "Поместите каталог приложения в /opt/zapomni/releases/." >&2; exit 1 ;;
esac
root=/opt/zapomni
config=/etc/zapomni/catalog.env
database_url='postgresql+psycopg://zapomni@/zapomni?host=/var/run/postgresql'

if ! id zapomni >/dev/null 2>&1; then
    useradd --system --user-group --no-create-home --shell /usr/sbin/nologin zapomni
fi
if [ "$(runuser -u postgres -- psql -Atqc "SELECT 1 FROM pg_roles WHERE rolname = 'zapomni'")" != 1 ]; then
    runuser -u postgres -- createuser zapomni
fi
if [ "$(runuser -u postgres -- psql -Atqc "SELECT 1 FROM pg_database WHERE datname = 'zapomni'")" != 1 ]; then
    runuser -u postgres -- createdb --owner=zapomni zapomni
fi

install -d -m 700 /etc/zapomni
if [ ! -e "$config" ]; then
    (
        umask 077
        python3 -c 'import secrets; print("EDITOR_API_KEY=" + secrets.token_urlsafe(32))'
        printf '%s\n' "DATABASE_URL=$database_url"
    ) > "$config"
    chmod 600 "$config"
fi

export UV_PYTHON_INSTALL_DIR="$root/python"
export UV_CACHE_DIR="$root/uv-cache"
cd "$release"
"$root/tooling/bin/uv" sync --locked --no-dev --python 3.13
runuser -u zapomni -- env "DATABASE_URL=$database_url" .venv/bin/alembic upgrade head
runuser -u zapomni -- env "DATABASE_URL=$database_url" .venv/bin/alembic check

install -m 644 deploy/zapomni.service /etc/systemd/system/zapomni.service
systemctl daemon-reload
systemctl enable zapomni.service
previous="$(readlink "$root/current" || true)"
next_link="$root/.current-$$"
ln -s "$release" "$next_link"
mv -Tf "$next_link" "$root/current"
if systemctl restart zapomni.service && curl --fail --silent --show-error --retry 5 \
    --retry-connrefused --retry-delay 1 --max-time 4 http://127.0.0.1:8000/api/v1/decks >/dev/null; then
    systemctl is-active zapomni.service
else
    if [ -n "$previous" ] && [ "$previous" != "$release" ]; then
        ln -s "$previous" "$next_link"
        mv -Tf "$next_link" "$root/current"
        systemctl restart zapomni.service
    fi
    exit 1
fi
