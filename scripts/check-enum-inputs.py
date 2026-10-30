#!/usr/bin/env python3
"""扫描"枚举被当自由文本用"的地方（AGENTS 13 / 13.1）。

两类高信号问题：
  1) 裸码列：<el-table-column prop="status|kind|purpose|channel|type|role">
     且该列没有自定义模板 —— 表格会直接显示 PRODUCT / SUCCEEDED 这类内部码。
  2) 手填枚举：<el-input ... placeholder="如 PAYMENT_REFUNDED"> 之类，
     候选本该是下拉（后端字典），却在让用户背常量。

用法：
  python3 scripts/check-enum-inputs.py            # 打印全部命中
  python3 scripts/check-enum-inputs.py --check    # 与白名单比对，未登记则退出码 1

白名单 docs/enum-input-allowlist.json：{"entries":[{"file","kind","snippet","reason"}]}
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ALLOWLIST = ROOT / 'docs' / 'enum-input-allowlist.json'
VIEWS = ROOT / 'web' / 'apps' / 'admin' / 'src'

RAW_CODE_COLUMNS = re.compile(
    r'<el-table-column\s+prop="(status|kind|purpose|channel|type|role|source)"', re.S)
ENUM_HINT = re.compile(r'(如|例如)\s*[A-Za-z][A-Za-z0-9_\- ]{2,}')
TECH_HINT = re.compile(r'ISO|IANA|逗号分隔|大写字母')
INPUT_TAG = re.compile(r'<el-input\b[^>]*>', re.S)


def scan() -> list:
    findings = []
    for path in VIEWS.rglob('*.vue'):
        text = path.read_text(encoding='utf-8')
        rel = str(path.relative_to(ROOT))

        for match in RAW_CODE_COLUMNS.finditer(text):
            tail = text[match.start():match.start() + 400]
            if '#default' in tail:
                continue
            line = text[:match.start()].count('\n') + 1
            findings.append((rel, line, 'raw-code-column', match.group(0).strip()))

        for match in INPUT_TAG.finditer(text):
            tag = match.group(0)
            placeholder = re.search(r'placeholder="([^"]*)"', tag)
            if not placeholder:
                continue
            value = placeholder.group(1)
            if ENUM_HINT.search(value) or TECH_HINT.search(value):
                line = text[:match.start()].count('\n') + 1
                findings.append((rel, line, 'hand-typed-enum', placeholder.group(0)))
    findings.sort()
    return findings


def fingerprint(file: str, kind: str, snippet: str) -> str:
    return f"{file}|{kind}|{snippet[:80]}"


def fp_of(finding) -> str:
    file, _line, kind, snippet = finding
    return fingerprint(file, kind, snippet)


def main() -> int:
    findings = scan()
    allow = {'entries': []}
    if ALLOWLIST.exists():
        allow = json.loads(ALLOWLIST.read_text(encoding='utf-8'))
    allowed = {fingerprint(e['file'], e['kind'], e.get('snippet', '')) for e in allow.get('entries', [])}

    if '--check' in sys.argv:
        unexpected = [f for f in findings if fp_of(f) not in allowed]
        if unexpected:
            print(f"发现 {len(unexpected)} 处枚举/裸码问题（AGENTS 13 / 13.1）：")
            for rel, line, kind, snippet in unexpected:
                print(f"  {rel}:{line}  [{kind}]  {snippet}")
            print("\n处理方式：改成下拉（候选来自后端 EnumCatalog）/ 用 enumLabel 渲染；"
                  "确属自由文本的登记到 docs/enum-input-allowlist.json 并写理由")
            return 1
        print(f"枚举检查通过（{len(findings)} 处已登记）")
        return 0

    for rel, line, kind, snippet in findings:
        print(f"{rel}:{line}  [{kind}]  {snippet}")
    print(f"\n共 {len(findings)} 处")
    return 0


if __name__ == '__main__':
    sys.exit(main())
