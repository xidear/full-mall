#!/bin/sh
set -eu

: "${DOMAIN:?DOMAIN is required (host only, no port)}"
: "${UPSTREAM:?UPSTREAM is required (storefront origin)}"
: "${API_UPSTREAM:?API_UPSTREAM is required (api origin)}"
: "${HTTPS_PORT:?HTTPS_PORT is required (public HTTPS port, e.g. 15443)}"

CERT_DIR="/etc/nginx/certs/${DOMAIN}"
mkdir -p "${CERT_DIR}" /var/www/acme/.well-known/acme-challenge

if [ ! -s "${CERT_DIR}/fullchain.pem" ] || [ ! -s "${CERT_DIR}/privkey.pem" ]; then
  echo "[edge] no certificate for ${DOMAIN}, generating self-signed placeholder"
  openssl req -x509 -nodes -newkey rsa:2048 -days 30 \
    -keyout "${CERT_DIR}/privkey.pem" \
    -out "${CERT_DIR}/fullchain.pem" \
    -subj "/CN=${DOMAIN}" \
    -addext "subjectAltName=DNS:${DOMAIN}"
fi

envsubst '${DOMAIN} ${UPSTREAM} ${API_UPSTREAM} ${HTTPS_PORT}' \
  < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf

nginx -g 'daemon off;' &
NGINX_PID=$!
last=""
while kill -0 "${NGINX_PID}" 2>/dev/null; do
  sleep 30
  current="$(stat -c %Y "${CERT_DIR}/fullchain.pem" 2>/dev/null || echo 0)"
  if [ "${current}" != "${last}" ]; then
    if nginx -t >/dev/null 2>&1; then
      nginx -s reload
      echo "[edge] reloaded nginx after certificate change"
      last="${current}"
    else
      echo "[edge] nginx config test failed; keeping old worker" >&2
    fi
  fi
done
wait "${NGINX_PID}"
