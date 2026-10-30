#!/usr/bin/env python3
"""扫描"静默兜底"：缺失被默认值/空实现掩盖的地方（AGENTS §1）。

三类：
  1) Java  @Value("${key:默认值}")        —— 配置缺失时代码自己编一个值
  2) Java  空 catch                        —— 失败被吞
  3) 前端  ?? '业务默认值' / || '业务默认值' / catch { } —— 缺失渲染成默认业务值

用法：
  python3 scripts/check-fallbacks.py            # 打印全部命中
  python3 scripts/check-fallbacks.py --check    # 与白名单比对，有未登记项则退出码 1（CI 用）

白名单 docs/fallback-allowlist.json：
  {"entries": [{"file": "...", "category": "value-default", "snippet": "...", "reason": "..."}]}
按 file + category + 片段指纹匹配，行号漂移不会误报。
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ALLOWLIST = ROOT / 'docs' / 'fallback-allowlist.json'

JAVA_VALUE_DEFAULT = re.compile(r'@Value\("\$\{[^}:]+:([^}]*)\}"\)')
TS_DEFAULT_ASSIGN = re.compile(r"(\?\?|\|\|)\s*'([^']{1,40})'")
EMPTY_CATCH_TS = re.compile(r'catch\s*(\([^)]*\))?\s*\{\s*\}')
JAVA_EMPTY_CATCH = re.compile(r'catch\s*\([^)]*\)\s*\{\s*\}', re.S)


def fingerprint(file: str, category: str, snippet: str) -> str:
    return f"{file}|{category}|{snippet.strip()[:60]}"


def scan_java(path: Path, out: list) -> None:
    text = path.read_text(encoding='utf-8', errors='ignore')
    rel = str(path.relative_to(ROOT))
    for match in JAVA_VALUE_DEFAULT.finditer(text):
        default = match.group(1).strip()
        if default:  # 空默认值表示"可为空"，不是兜底；给了具体值才是
            line = text[:match.start()].count('\n') + 1
            out.append((rel, line, 'value-default', match.group(0)))
    for match in JAVA_EMPTY_CATCH.finditer(text):
        line = text[:match.start()].count('\n') + 1
        out.append((rel, line, 'empty-catch', 'catch (...) { }'))


def scan_frontend(path: Path, out: list) -> None:
    text = path.read_text(encoding='utf-8', errors='ignore')
    rel = str(path.relative_to(ROOT))
    for match in TS_DEFAULT_ASSIGN.finditer(text):
        value = match.group(2)
        if not value or value in ('-', '—', '/', '#'):
            continue
        # 只关心"会改变业务行为"的默认值：URL / 端口 / 币种 / 供应商 / 状态码这类 ASCII 配置值。
        # 中文文本（如 || '未留手机号'）属于展示兜底，不在此列——它不会让系统以为配置已就绪。
        if re.search(r'[一-鿿]', value):
            continue
        if not re.search(r'[A-Za-z]', value):
            continue
        line = text[:match.start()].count('\n') + 1
        out.append((rel, line, 'value-default', match.group(0)))
    for match in EMPTY_CATCH_TS.finditer(text):
        line = text[:match.start()].count('\n') + 1
        out.append((rel, line, 'empty-catch', 'catch { }'))


def main() -> int:
    findings = []
    for path in (ROOT / 'server' / 'src' / 'main' / 'java').rglob('*.java'):
        scan_java(path, findings)
    for pattern in ('*.ts', '*.vue'):
        for path in (ROOT / 'web' / 'apps').rglob(pattern):
            if 'node_modules' in str(path) or '.nuxt' in str(path):
                continue
            scan_frontend(path, findings)

    findings.sort()
    allow = {'entries': []}
    if ALLOWLIST.exists():
        allow = json.loads(ALLOWLIST.read_text(encoding='utf-8'))
    allowed = {fingerprint(e['file'], e['category'], e.get('snippet', '')) for e in allow.get('entries', [])}

    if '--check' in sys.argv:
        unexpected = [f for f in findings if fingerprint(f[0], f[2], f[3]) not in allowed]
        if unexpected:
            print(f"发现 {len(unexpected)} 处未登记的兜底写法（AGENTS §1）：")
            for rel, line, category, snippet in unexpected:
                print(f"  {rel}:{line}  [{category}]  {snippet}")
            print("\n处理方式：删掉默认值 / 明确失败；确属部署参数，登记到 docs/fallback-allowlist.json")
            return 1
        print(f"兜底检查通过（{len(findings)} 处已登记）")
        return 0

    for rel, line, category, snippet in findings:
        print(f"{rel}:{line}  [{category}]  {snippet}")
    print(f"\n共 {len(findings)} 处")
    return 0


if __name__ == '__main__':
    sys.exit(main())
