#!/bin/sh
# 从库启动：等待主库 -> 首次 pg_basebackup -> 启动 postgres。
set -eu
: "${PRIMARY_HOST:?}"
: "${PRIMARY_PORT:-5432}"
: "${POSTGRES_REPLICATION_USER:?}"
: "${POSTGRES_REPLICATION_PASSWORD:?}"
PGDATA="${PGDATA:-/var/lib/postgresql/data}"

until pg_isready -h "$PRIMARY_HOST" -p "$PRIMARY_PORT" -U "$POSTGRES_REPLICATION_USER" >/dev/null 2>&1; do
  echo "[replica] waiting for primary $PRIMARY_HOST:$PRIMARY_PORT ..."
  sleep 2
done

if [ ! -s "$PGDATA/PG_VERSION" ]; then
  echo "[replica] basebackup from $PRIMARY_HOST:$PRIMARY_PORT"
  mkdir -p "$PGDATA"
  chown postgres:postgres "$PGDATA"
  PGPASSWORD="$POSTGRES_REPLICATION_PASSWORD" pg_basebackup     -h "$PRIMARY_HOST" -p "$PRIMARY_PORT"     -U "$POSTGRES_REPLICATION_USER"     -D "$PGDATA" -Fp -Xs -P -R
  chown -R postgres:postgres "$PGDATA"
  chmod 700 "$PGDATA"
fi

# Docker volume 根目录默认 1777；PostgreSQL 17 要求 0700/0750，启动前再兜底一次。
chown postgres:postgres "$PGDATA"
chmod 700 "$PGDATA"

exec gosu postgres postgres
