#!/usr/bin/env bash
set -euo pipefail

public_ip="$(python3 -c 'from ipaddress import IPv4Address; import sys; print(IPv4Address(sys.argv[1]))' "$1")"
templates="$(cd "$(dirname "$0")" && pwd -P)"
site=/etc/nginx/sites-available/zapomni
enabled=/etc/nginx/sites-enabled/zapomni
acme_root=/var/lib/zapomni/acme
certificate="/etc/letsencrypt/live/$public_ip/fullchain.pem"
certbot=/opt/zapomni/tooling/bin/certbot

install -d -m 755 "$acme_root"
if [ ! -s "$certificate" ]; then
    sed -e "s/@PUBLIC_IP@/$public_ip/g" -e 's/@HTTP_RESPONSE@/return 503;/g' \
        "$templates/nginx-http.conf" > "$site"
    ln -sfn "$site" "$enabled"
    nginx -t
    systemctl reload nginx
    "$certbot" certonly --non-interactive --agree-tos --register-unsafely-without-email \
        --webroot -w "$acme_root" --ip-address "$public_ip" --required-profile shortlived \
        --cert-name "$public_ip"
fi

{
    sed -e "s/@PUBLIC_IP@/$public_ip/g" \
        -e "s|@HTTP_RESPONSE@|return 308 https://$public_ip\$request_uri;|g" "$templates/nginx-http.conf"
    sed "s/@PUBLIC_IP@/$public_ip/g" "$templates/nginx-https.conf"
} > "$site"
ln -sfn "$site" "$enabled"
nginx -t
systemctl reload nginx

install -m 644 "$templates/zapomni-certbot.service" "$templates/zapomni-certbot.timer" /etc/systemd/system/
systemctl daemon-reload
systemctl enable --now zapomni-certbot.timer
