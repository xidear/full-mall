#!/usr/bin/env bash
# 迁移"一次成型"收敛脚本（未上线专用）：148 个增量迁移 → 一个基线 + 一份 seed。
#
# 为什么两份文件：schema 与 seed 分开，重抓数据（区划、默认设置）时不必重导 schema；
# 也避免"先建表再加列 / 先加后删"的历史噪音。
#
# 用法：
#   scripts/squash-migrations.sh --dry-run   # 只在临时库验证"能从零建起来"，不改文件
#   scripts/squash-migrations.sh --apply     # 生成 V1__baseline.sql + V2__seed.sql，删除其余迁移
#
# 重要：pg_dump **不能**只导 schema——权限字典/角色/区划等 seed 数据都写在迁移里，
# 只导 schema 会让新库变成空权限表（实测踩过）。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MIG_DIR="$ROOT/server/src/main/resources/db/migration"
PG_CONTAINER="${PG_CONTAINER:-fullmall-postgres-1}"
PG_USER="${PG_USER:-fullmall}"
SCRATCH_DB="${SCRATCH_DB:-fullmall_squash}"
VERIFY_DB="${SCRATCH_DB}_verify"
MODE="${1:---dry-run}"

echo "== 1/5 用现有迁移在临时库 $SCRATCH_DB 上从零建库 =="
docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -q \
  -c "DROP DATABASE IF EXISTS $SCRATCH_DB" -c "CREATE DATABASE $SCRATCH_DB"
count=0
for f in $(ls "$MIG_DIR"/*.sql | sort -V); do
  docker exec -i "$PG_CONTAINER" psql -U "$PG_USER" -d "$SCRATCH_DB" -v ON_ERROR_STOP=1 -q < "$f"
  count=$((count + 1))
done
echo "   已应用 $count 个迁移"

q() { docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$SCRATCH_DB" -t -A -c "$1"; }
TABLES=$(q "select count(*) from information_schema.tables where table_schema='public'")
PERMS=$(q "select count(*) from permission")
echo "   表=$TABLES 权限=$PERMS"

if [[ "$MODE" == "--dry-run" ]]; then
  echo "== dry-run 结束：链路可用，未修改任何文件 =="
  exit 0
fi

echo "== 2/5 导出 schema + seed =="
docker exec "$PG_CONTAINER" pg_dump -U "$PG_USER" -d "$SCRATCH_DB" --schema-only --no-owner --no-privileges > /tmp/sq_schema.sql
docker exec "$PG_CONTAINER" pg_dump -U "$PG_USER" -d "$SCRATCH_DB" --data-only --column-inserts --no-owner --no-privileges > /tmp/sq_seed.sql
strip() { grep -v '^--' "$1" | grep -v '^SET ' | grep -v '^SELECT pg_catalog.set_config'; }
{
  echo "-- 基线 schema：由 scripts/squash-migrations.sh 从"仅用旧迁移建出的库"导出（项目未上线，一次性初始化）。"
  echo "-- 结构改动请直接改本文件；上线后再用增量迁移（V3 起）。seed 数据见 V2__seed.sql。"
  strip /tmp/sq_schema.sql
} > "$MIG_DIR/V1__baseline.sql"
{
  echo "-- 基线 seed：权限字典/角色/区划/默认设置等"写在迁移里"的数据（pg_dump --data-only）。"
  echo "-- 重抓数据（如区划）请覆盖本文件，不要新增迁移。"
  echo "SET session_replication_role = replica;"
  strip /tmp/sq_seed.sql
  echo "SET session_replication_role = DEFAULT;"
} > "$MIG_DIR/V2__seed.sql"

echo "== 2.5/5 补 ADR-0015 的 role 表（DDL→V1，seed→V2） =="
python3 - "$MIG_DIR" <<'PY'
import sys
from pathlib import Path
mig = Path(sys.argv[1])
base = mig / 'V1__baseline.sql'
seed = mig / 'V2__seed.sql'
sql = base.read_text(encoding='utf-8')
if 'CREATE TABLE public.role (' not in sql:
    ddl = """CREATE TABLE public.role (
    code character varying(32) NOT NULL,
    label character varying(64) NOT NULL,
    editable boolean DEFAULT true NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL
);

ALTER TABLE ONLY public.role
    ADD CONSTRAINT role_pkey PRIMARY KEY (code);

"""
    anchor = 'CREATE TABLE public.staff_user ('
    assert anchor in sql, 'staff_user DDL 缺失'
    sql = sql.replace(anchor, ddl + anchor, 1)
    sql = sql.rstrip() + """

ALTER TABLE ONLY public.staff_user
    ADD CONSTRAINT staff_user_role_fk FOREIGN KEY (role) REFERENCES public.role(code);
"""
    base.write_text(sql, encoding='utf-8')

s = seed.read_text(encoding='utf-8')
if 'INSERT INTO public.role ' not in s:
    rows = [('ADMIN', '超级管理员', 'false', 10), ('OPERATOR', '运营人员', 'true', 20),
            ('STORE', '门店人员', 'true', 30), ('CS', '客服人员', 'true', 40),
            ('SUPPLIER', '供应商', 'false', 50)]
    inserts = ''.join(
        "INSERT INTO public.role (code, label, editable, sort_order) VALUES ('%s', '%s', %s, %d);\n" % r
        for r in rows)
    marker = 'SET session_replication_role = DEFAULT;'
    s = s.replace(marker, inserts + marker, 1)
    seed.write_text(s, encoding='utf-8')
print('role 表已补齐')
PY

echo "== 3/5 删除旧迁移（保留 V1 + V2） =="
find "$MIG_DIR" -maxdepth 1 -name 'V*__*.sql' ! -name 'V1__baseline.sql' ! -name 'V2__seed.sql' -delete

echo "== 4/5 用新基线重建临时库 =="
docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d postgres -q \
  -c "DROP DATABASE IF EXISTS $VERIFY_DB" -c "CREATE DATABASE $VERIFY_DB"
for f in "$MIG_DIR"/V1__baseline.sql "$MIG_DIR"/V2__seed.sql; do
  docker exec -i "$PG_CONTAINER" psql -U "$PG_USER" -d "$VERIFY_DB" -v ON_ERROR_STOP=1 -q < "$f"
done

echo "== 5/5 核对表数与权限行数 =="
qv() { docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$VERIFY_DB" -t -A -c "$1"; }
NEW_TABLES=$(qv "select count(*) from information_schema.tables where table_schema='public'")
NEW_PERMS=$(qv "select count(*) from permission")
echo "   表=$NEW_TABLES（应 $TABLES） 权限=$NEW_PERMS（应 $PERMS）"
if [[ "$NEW_TABLES" != "$TABLES" || "$NEW_PERMS" != "$PERMS" ]]; then
  echo "!! 基线不完整：请 git checkout server/src/main/resources/db/migration 回滚" >&2
  exit 1
fi
echo "完成。任何已存在的库都要重建（Flyway 历史与旧迁移文件已不兼容）。"
