#!/usr/bin/env bash
# 在主库初始化时创建流复制账号，并允许来自容器的 replication 连接。
set -Eeuo pipefail
: "${POSTGRES_USER:?}"
: "${POSTGRES_DB:?}"
: "${POSTGRES_REPLICATION_USER:?}"
: "${POSTGRES_REPLICATION_PASSWORD:?}"

password_sql="$(printf '%s' "$POSTGRES_REPLICATION_PASSWORD" | sed "s/'/''/g")"
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<SQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${POSTGRES_REPLICATION_USER}') THEN
    CREATE ROLE "${POSTGRES_REPLICATION_USER}" WITH REPLICATION LOGIN PASSWORD '${password_sql}';
  END IF;
END
\$\$;
SQL

if ! grep -qE "^host[[:space:]]+replication[[:space:]]+${POSTGRES_REPLICATION_USER}" "$PGDATA/pg_hba.conf"; then
  echo "host replication ${POSTGRES_REPLICATION_USER} 0.0.0.0/0 scram-sha-256" >> "$PGDATA/pg_hba.conf"
fi
