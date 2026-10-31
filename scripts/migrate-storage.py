#!/usr/bin/env python3
"""素材文件迁移与一致性校验（本机磁盘 <-> 本机磁盘）。

为什么需要：切换存储服务商时，库里 asset_object 记录还在，文件却留在旧目录/旧云端，
表现为"素材库有记录、图片全裂"。这个脚本把"文件到底在不在、对不对"变成可执行检查。

用法：
  python3 scripts/migrate-storage.py --verify --dir /data/fullmall/server/data/assets
  python3 scripts/migrate-storage.py --copy --from /old/assets --to /new/assets [--apply]

范围（如实说明）：
  支持本机磁盘目录之间迁移（同机换目录、换挂载盘）。
  跨云（local <-> COS/七牛）需要各家 SDK 与凭据，本脚本不做，会明确报错退出，
  而不是"静默跳过"让你以为迁完了。
"""
import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

DB_CONTAINER = 'fullmall-postgres-1'


def query_object_keys() -> list:
    sql = "SELECT object_key FROM asset_object WHERE storage = 'local' ORDER BY object_key"
    out = subprocess.run(
        ['docker', 'exec', DB_CONTAINER, 'psql', '-U', 'fullmall', '-d', 'fullmall', '-t', '-A', '-c', sql],
        capture_output=True, text=True)
    if out.returncode != 0:
        print('读取 asset_object 失败：' + out.stderr.strip(), file=sys.stderr)
        sys.exit(1)
    return [line.strip() for line in out.stdout.splitlines() if line.strip()]


def sha256_of(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open('rb') as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def verify(directory: Path) -> int:
    keys = query_object_keys()
    if not keys:
        print('asset_object 里没有 local 存储的记录，无需校验')
        return 0
    missing, empty = [], []
    for key in keys:
        target = directory / key
        if not target.exists():
            missing.append(key)
        elif target.stat().st_size == 0:
            empty.append(key)
    print(f'库内 local 记录 {len(keys)} 条；缺失 {len(missing)}；空文件 {len(empty)}')
    for key in missing[:10]:
        print('  缺失:', key)
    for key in empty[:10]:
        print('  空文件:', key)
    return 1 if (missing or empty) else 0


def copy(src: Path, dst: Path, apply: bool) -> int:
    keys = query_object_keys()
    if not keys:
        print('asset_object 里没有 local 存储的记录，无需迁移')
        return 0
    plan, missing_src, mismatched = [], [], []
    for key in keys:
        source = src / key
        if not source.exists():
            missing_src.append(key)
            continue
        target = dst / key
        if target.exists() and sha256_of(target) == sha256_of(source):
            continue
        plan.append((key, source, target))
    print(f'待迁移 {len(plan)} 个文件；源端缺失 {len(missing_src)}')
    for key in missing_src[:10]:
        print('  源端缺失:', key)
    if not apply:
        for key, _s, _t in plan[:10]:
            print('  将复制:', key)
        print('（--apply 才会真正写入）')
        return 0 if not missing_src else 1
    for key, source, target in plan:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(source.read_bytes())
        if sha256_of(target) != sha256_of(source):
            mismatched.append(key)
    print(f'已复制 {len(plan) - len(mismatched)} 个；sha256 不一致 {len(mismatched)}')
    for key in mismatched[:10]:
        print('  校验失败:', key)
    return 0 if (not mismatched and not missing_src) else 1


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--verify', action='store_true', help='校验库内记录与目录内文件是否一致')
    parser.add_argument('--copy', action='store_true', help='把文件从 --from 复制到 --to')
    parser.add_argument('--from', dest='src', help='源目录')
    parser.add_argument('--to', dest='dst', help='目标目录')
    parser.add_argument('--dir', help='校验时使用的目录')
    parser.add_argument('--apply', action='store_true', help='真正写入（否则只打印计划）')
    args = parser.parse_args()

    if args.verify:
        if not args.dir:
            print('--verify 需要 --dir', file=sys.stderr)
            return 2
        return verify(Path(args.dir).resolve())

    if args.copy:
        if not args.src or not args.dst:
            print('--copy 需要 --from 与 --to', file=sys.stderr)
            return 2
        return copy(Path(args.src).resolve(), Path(args.dst).resolve(), args.apply)

    print('跨云迁移（local <-> COS/七牛）需要各云 SDK 与凭据，本脚本不支持；'
          '请使用对应云厂商工具，并用 --verify 核对结果', file=sys.stderr)
    parser.print_help()
    return 2


if __name__ == '__main__':
    sys.exit(main())
