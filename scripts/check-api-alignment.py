#!/usr/bin/env python3
"""前端调用与后端路由的对齐检查。

为什么需要：前端页面写死 `/api/v1/...`，后端路径由类级 + 方法级注解拼接；
任何一侧改名都会 404，而编译与单测都不会发现。

用法：python3 scripts/check-api-alignment.py [--verbose]
退出码：0 全部匹配；1 存在前端调用但后端无匹配路由。
"""
from __future__ import annotations

import glob
import re
import sys

MAPPING = re.compile(r"@(Get|Post|Put|Delete|Patch|Request)Mapping(?:\s*\(\s*(?:value\s*=\s*)?\"([^\"]*)\"[^)]*\))?")
VAR = re.compile(r"\{[^}]*\}")


def normalize(path: str) -> str:
    path = path.split("?")[0]
    path = re.sub(r"\$\{[^}]*\}", "{}", path)        # 前端模板变量 ${x}
    path = re.sub(r"\$\{[^}]*$", "", path)          # 被截断的模板（正则匹配边界）
    path = VAR.sub("{}", path)                # 后端路径变量
    path = re.sub(r"/{2,}", "/", path).rstrip("/")
    return path or "/"


def backend_routes() -> set[str]:
    routes: set[str] = set()
    for file in glob.glob("server/src/main/java/**/*.java", recursive=True):
        source = open(file, encoding="utf-8").read()
        class_level = ""
        class_match = re.search(r'@RequestMapping\(\s*(?:value\s*=\s*)?"([^"]+)"', source)
        if class_match:
            class_level = class_match.group(1)
        for match in MAPPING.finditer(source):
            kind, value = match.group(1), match.group(2) or ""
            if kind == "Request" and value == class_level:
                continue  # 类级注解本身不是路由
            if kind == "Request":
                continue
            routes.add(normalize(class_level + "/" + value))
    return routes


def frontend_calls() -> dict[str, set[str]]:
    calls: dict[str, set[str]] = {}
    patterns = ("web/apps/**/*.vue", "web/apps/**/*.ts", "web/packages/**/*.ts")
    for pattern in patterns:
        for file in glob.glob(pattern, recursive=True):
            source = open(file, encoding="utf-8").read()
            for match in re.finditer(r"/api/v1/[A-Za-z0-9_\-/{}$.]+", source):
                path = normalize(match.group(0))
                calls.setdefault(path, set()).add(file)
    return calls


def main() -> int:
    verbose = "--verbose" in sys.argv
    routes = backend_routes()
    calls = frontend_calls()
    # 前缀匹配也算命中：类级路由 "/api/v1/products" 会承接 "/api/v1/products/{}/reviews"
    def matched(path: str) -> bool:
        # 双向：前端可能只写到集合路径（/orders），后端是 /orders/{id}
        return path in routes or any(
            path.startswith(route.rstrip("/") + "/") or route.startswith(path.rstrip("/") + "/")
            for route in routes if route != "/")

    missing = {path: files for path, files in calls.items() if not matched(path)}

    print(f"后端路由 {len(routes)} 条；前端调用 {len(calls)} 条；未匹配 {len(missing)} 条")
    if verbose:
        for path in sorted(routes):
            print(f"  backend  {path}")
    for path, files in sorted(missing.items()):
        print(f"  未匹配  {path}")
        for file in sorted(files):
            print(f"           <- {file}")
    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main())
