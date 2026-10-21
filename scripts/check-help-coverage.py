#!/usr/bin/env python3
"""后台帮助覆盖率检查 + 帮助索引生成。

规则（任何一条不满足即非零退出）：
1. `web/apps/admin/src/router.ts` 里每个非参数化路由，必须在 `src/help/catalog.ts` 有对应条目；
2. 帮助条目不能指向不存在的路由（防止删页面后留死链）；
3. 每条帮助必须有 title / description / steps / tips / pitfalls，且 steps 与 tips 非空；
4. `docs/HELP_INDEX.md` 必须与当前帮助内容一致（--write 时重新生成）。

用法：
    python3 scripts/check-help-coverage.py            # 只检查
    python3 scripts/check-help-coverage.py --write     # 重新生成索引后再检查
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ROUTER = ROOT / "web/apps/admin/src/router.ts"
CATALOG = ROOT / "web/apps/admin/src/help/catalog.ts"
INDEX = ROOT / "docs/HELP_INDEX.md"

EXCLUDED = {"/login"}


def parse_routes() -> list[str]:
    text = ROUTER.read_text(encoding="utf-8")
    routes = re.findall(r"path:\s*'([^']+)'", text)
    return [r for r in routes if ":" not in r and r not in EXCLUDED]


def parse_catalog() -> dict[str, dict]:
    text = CATALOG.read_text(encoding="utf-8")
    entries: dict[str, dict] = {}
    # 按 route 切块，再抓取各字段
    for block in re.split(r"\n  \{\n", text):
        route_match = re.search(r"route:\s*'([^']+)'", block)
        if not route_match:
            continue
        route = route_match.group(1)
        def field(name: str) -> str:
            m = re.search(rf"{name}:\s*'([^']*)'", block)
            return m.group(1) if m else ""
        def list_field(name: str) -> list[str]:
            m = re.search(rf"{name}:\s*\[(.*?)\]", block, re.S)
            if not m:
                return []
            return re.findall(r"'([^']*)'", m.group(1))
        entries[route] = {
            "title": field("title"),
            "description": field("description"),
            "steps": list_field("steps"),
            "tips": list_field("tips"),
            "pitfalls": list_field("pitfalls"),
            "related": list_field("related"),
        }
    return entries


def render_index(routes: list[str], catalog: dict[str, dict]) -> str:
    lines = [
        "# 后台帮助索引",
        "",
        "> 本文件由 `scripts/check-help-coverage.py --write` 生成，**请勿手改**。",
        "> 帮助内容来源：`web/apps/admin/src/help/catalog.ts`；覆盖率由脚本在验证流程中强制检查。",
        "",
        f"共 {len(catalog)} 个页面帮助。",
        "",
        "| 页面 | 标题 | 说明 |",
        "|---|---|---|",
    ]
    for route in routes:
        entry = catalog.get(route)
        if entry:
            lines.append(f"| `{route}` | {entry['title']} | {entry['description']} |")
    lines.append("")
    for route in routes:
        entry = catalog.get(route)
        if not entry:
            continue
        lines.append(f"## {entry['title']}（`{route}`）")
        lines.append("")
        lines.append(entry["description"])
        lines.append("")
        lines.append("**操作步骤**")
        lines.append("")
        for step in entry["steps"]:
            lines.append(f"1. {step}")
        lines.append("")
        lines.append("**运营建议**")
        lines.append("")
        for tip in entry["tips"]:
            lines.append(f"- {tip}")
        lines.append("")
        lines.append("**常见错误**")
        lines.append("")
        for pitfall in entry["pitfalls"]:
            lines.append(f"- {pitfall}")
        lines.append("")
    return "\n".join(lines)


def main() -> int:
    write = "--write" in sys.argv
    routes = parse_routes()
    catalog = parse_catalog()
    problems: list[str] = []

    for route in routes:
        if route not in catalog:
            problems.append(f"缺少帮助：{route}（router.ts 有页面，catalog.ts 没有条目）")
    for route in catalog:
        if route not in routes:
            problems.append(f"死链帮助：{route}（catalog.ts 有条目，router.ts 没有该路由）")
    for route, entry in catalog.items():
        for field in ("title", "description"):
            if not entry[field].strip():
                problems.append(f"{route} 的 {field} 为空")
        for field in ("steps", "tips", "pitfalls"):
            if not entry[field]:
                problems.append(f"{route} 的 {field} 为空（至少要有一条）")
        for link in entry["related"]:
            if link not in routes:
                problems.append(f"{route} 的 related 指向不存在的路由：{link}")

    rendered = render_index(routes, catalog)
    if write:
        INDEX.write_text(rendered + "\n", encoding="utf-8")
        print(f"已生成 {INDEX.relative_to(ROOT)}")
    elif not INDEX.exists():
        problems.append("docs/HELP_INDEX.md 不存在，请运行 --write 生成")
    elif INDEX.read_text(encoding="utf-8").strip() != rendered.strip():
        problems.append("docs/HELP_INDEX.md 与帮助内容不一致，请运行 --write 重新生成")

    if problems:
        print("帮助检查未通过：", file=sys.stderr)
        for problem in problems:
            print(f"  - {problem}", file=sys.stderr)
        return 1

    print(f"帮助检查通过：{len(catalog)} 个页面帮助，索引已同步")
    return 0


if __name__ == "__main__":
    sys.exit(main())
