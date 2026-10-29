#!/usr/bin/env python3
"""前端调用与后端路由的对齐检查（方法 + 路径，覆盖所有 API 前缀）。

为什么需要：前端写死 /admin/v1/... 或 /api/v1/...，后端路径由类级 + 方法级注解拼接；
任何一侧改名、漏实现或方法写错，只有运行时才暴露（404 / 405），编译和单测都发现不了。

历史教训（2026-10-28）：
- 旧版本只扫 /api/v1/ 前缀、且不区分 HTTP 方法，于是
  GET /admin/v1/inventory/batches/expiring 这类"前端调用、后端没实现"的问题全部漏过；
- 现在覆盖所有前缀 + 方法，并把"社区版故意 404"的接口列入白名单（写明原因）。

用法：python3 scripts/check-api-alignment.py [--verbose]
退出码：0 全部匹配；1 存在前端调用但后端没有对应"方法 + 路径"。

扫描范围（它是“拦住前端调用不存在的后端路由”的唯一闸门，不是全量覆盖）：
- 后端：server/src/main/java/**/*.java，类级 @RequestMapping 前缀 + 方法级注解拼接；
- 前端：web/apps/**/*.vue|*.ts 与 web/packages/**/*.ts，排除 node_modules 与 /dist/；
- 只比对「方法 + 路径」，不比对请求体/参数名；动态拼出的路径、或封装成自定义方法名的调用会漏检。

失败输出的含义：每条「未匹配」下面是调用它的前端文件。修法是补后端接口或改前端；
确实故意 404 的（社区版砍掉的模块）必须写进 ALLOWLIST 并注明原因，否则下次还会报。
退出码非 0 即失败；CI/预提交必须直接传该退出码，不能用 `|| true` 吞掉。
"""
from __future__ import annotations

import glob
import re
import sys

API_PREFIXES = ("/api/", "/admin/", "/store/", "/supplier/", "/open/", "/support/", "/mock/")

CALL = re.compile(r"\.(get|post|put|patch|delete)\s*(?:<[^>\n]*>)?\s*\(")
NAMED_CALL = re.compile(
    r"\b(?:signedRequest|request|apiRequest|fetch)\s*(?:<[^>\n]*>)?\s*\(\s*['\"](GET|POST|PUT|PATCH|DELETE)['\"]\s*,",
    re.IGNORECASE)

# 400 是「类注解 → class 关键字」的最大跨度：注解里塞多行参数时不够会漏扫，改大只会变慢。
# 允许全限定写法：@org.springframework.web.bind.annotation.GetMapping("/x")
CLASS_PREFIX = re.compile(
    r'@(?:[\w.]+\.)?RequestMapping\(\s*(?:value\s*=\s*)?"([^"]*)"[^\n]*\)[\s\S]{0,400}?class\s+\w+')
METHOD_MAPPING = re.compile(
    r"@(?:[\w.]+\.)?(Get|Post|Put|Patch|Delete)Mapping(?:\s*\(\s*(?:value\s*=\s*)?" r'"([^"]*)"[^)]*\))?')

ALLOWLIST = {
    "GET /admin/v1/ai/status": "社区版无 AI 模块，AiView 用 404 判定并提示",
    "POST /admin/v1/ai/ask": "社区版无 AI 模块，随 status 一起 404",
    "POST /admin/v1/ai/knowledge/import": "知识库导入属于企业版",
}


def normalize(path: str) -> str:
    """归一路径：去 query、模板变量统一成 {}、折叠重复斜杠、去尾部斜杠（根路径保留 /）。"""
    path = path.split("?")[0]
    path = re.sub(r"\$\{[^}]*\}", "{}", path)
    path = re.sub(r"\$\{[^}]*$", "{}", path)   # 被截断的模板（扫描边界）
    path = re.sub(r"\{[^}]*\}", "{}", path)
    path = re.sub(r"/{2,}", "/", path)
    return path.rstrip("/") or "/"


def segments_match(call_path: str, route_path: str) -> bool:
    """段级比较：任一侧是 {} 都视为匹配该段（前端把 'review'/'publish' 之类的动作拼在变量里）。"""
    call_parts = call_path.strip("/").split("/")
    route_parts = route_path.strip("/").split("/")
    if len(call_parts) != len(route_parts):
        return False
    return all(a == b or a == "{}" or b == "{}" for a, b in zip(call_parts, route_parts))


def call_arg_text(source: str, open_paren: int) -> str:
    """取调用括号内的第一个参数：字符串内部的括号与逗号不参与配对。"""
    depth = 1
    i = open_paren + 1
    out: list[str] = []
    quotes = "'\"" + chr(96)
    while i < len(source):
        ch = source[i]
        if ch in quotes:
            quote = ch
            out.append(ch)
            i += 1
            while i < len(source):
                if source[i] == "\\":
                    out.append(source[i])
                    if i + 1 < len(source):
                        out.append(source[i + 1])
                        i += 2
                        continue
                if quote == chr(96) and source.startswith("${", i):
                    inner = 0
                    while i < len(source):
                        out.append(source[i])
                        if source[i] == "{":
                            inner += 1
                        elif source[i] == "}":
                            inner -= 1
                            if inner == 0:
                                i += 1
                                break
                        i += 1
                    continue
                out.append(source[i])
                if source[i] == quote:
                    i += 1
                    break
                i += 1
            continue
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            depth -= 1
            if depth == 0:
                break
        elif ch == "," and depth == 1:
            break
        out.append(ch)
        i += 1
    return "".join(out)


def path_template(text: str) -> str:
    """把首个参数表达式转成路径模板：字符串拼接与模板变量都变成 {}。"""
    parts: list[str] = []
    i = 0
    backtick = chr(96)  # 模板字符串定界符
    while i < len(text):
        ch = text[i]
        if ch in "'\"":
            quote = ch
            j = i + 1
            buf: list[str] = []
            while j < len(text):
                if text[j] == "\\":
                    buf.append(text[j + 1])
                    j += 2
                    continue
                if text[j] == quote:
                    break
                buf.append(text[j])
                j += 1
            parts.append("".join(buf))
            i = j + 1
            k = i
            while k < len(text) and text[k] in " \n\t":
                k += 1
            if k < len(text) and text[k] == "+":
                parts.append("{}")
                k += 1
                depth = 0
                while k < len(text):
                    ch2 = text[k]
                    if ch2 in "([":
                        depth += 1
                    elif ch2 in ")]":
                        if depth == 0:
                            break
                        depth -= 1
                    elif ch2 == "+" and depth == 0:
                        break
                    k += 1
                i = k
            continue
        if ch == backtick:
            j = i + 1
            buf = []
            while j < len(text):
                if text[j] == backtick:
                    break
                buf.append(text[j])
                j += 1
            parts.append("".join(buf))
            i = j + 1
            continue
        i += 1
    return "".join(parts)


def backend_routes() -> set[str]:
    """扫描后端源码，得到 {METHOD /path} 集合。

    类级前缀取「该注解之前最近的一个 @RequestMapping」：把类注解写在方法之后、
    或用嵌套类都会错配——出现漏报/误报时先查这里，而不是先怀疑前端。
    """
    routes: set[str] = set()
    for file in glob.glob("server/src/main/java/**/*.java", recursive=True):
        source = open(file, encoding="utf-8").read()
        prefixes = [(m.start(), m.group(1)) for m in CLASS_PREFIX.finditer(source)]
        for match in METHOD_MAPPING.finditer(source):
            prefix = ""
            for position, path in prefixes:
                if position < match.start():
                    prefix = path
                else:
                    break
            method = match.group(1).upper()
            routes.add(f"{method} {normalize(prefix + '/' + (match.group(2) or ''))}")
    return routes


def frontend_calls() -> dict[str, set[str]]:
    """扫描前端源码，得到 {METHOD 路径模板: {调用文件}}。

    CALL 抓 .get(/.post( 这类链式调用；NAMED_CALL 抓 signedRequest/request/apiRequest/fetch('GET', …)
    这种显式传方法的封装。两种都抓不到的新写法必须补正则，否则该接口等于没被检查。
    """
    calls: dict[str, set[str]] = {}
    files: list[str] = []
    for pattern in ("web/apps/**/*.vue", "web/apps/**/*.ts", "web/packages/**/*.ts"):
        files.extend(glob.glob(pattern, recursive=True))
    for file in files:
        if "node_modules" in file or "/dist/" in file:
            continue
        source = open(file, encoding="utf-8").read()
        found: list[tuple[int, str]] = []
        for match in CALL.finditer(source):
            found.append((match.end() - 1, match.group(1).upper()))
        for match in NAMED_CALL.finditer(source):
            found.append((match.end() - 1, match.group(1).upper()))
        for paren, method in found:
            template = normalize(path_template(call_arg_text(source, paren)))
            if not template.startswith(API_PREFIXES):
                continue
            calls.setdefault(f"{method} {template}", set()).add(file)
    return calls


def main() -> int:
    """打印统计与未匹配清单；返回进程退出码（0 = 全部匹配，1 = 有前端调用没有对应后端路由）。"""
    verbose = "--verbose" in sys.argv
    routes = backend_routes()
    calls = frontend_calls()

    def matched(key: str) -> bool:
        if key in routes:
            return True
        method, path = key.split(" ", 1)
        call_parts = path.strip('/').split('/')
        for route in routes:
            if not route.startswith(method + " "):
                continue
            route_path = route.split(" ", 1)[1]
            if segments_match(path, route_path):
                return True
            # 前端把查询串当路径片段拼接（/orders + '?status=...' / /faq + '?keyword=...'）：
            # 末尾的 {} 可能是"查询串"，允许去掉它（整段或段尾）再比一次
            if call_parts and call_parts[-1].endswith("{}"):
                tail = call_parts[-1][:-2]
                shorter = '/' + '/'.join(call_parts[:-1] + ([tail] if tail else []))
                if segments_match(shorter, route_path):
                    return True
                if tail and segments_match(shorter, route_path):
                    return True
        return False

    missing = {key: files for key, files in calls.items()
               if not matched(key) and key not in ALLOWLIST}
    allowed = {key: files for key, files in calls.items() if key in ALLOWLIST and key not in routes}

    print(f"后端路由 {len(routes)} 条；前端调用 {len(calls)} 条；未匹配 {len(missing)} 条")
    if verbose:
        for key in sorted(routes):
            print(f"  backend  {key}")
        for key in sorted(allowed):
            print(f"  allowed  {key}  ({ALLOWLIST[key]})")
    for key, files in sorted(missing.items()):
        print(f"  未匹配  {key}")
        for file in sorted(files):
            print(f"           <- {file}")
    if missing:
        print("\n处理方式：补后端接口，或修前端路径/方法；确实故意 404 的写进 ALLOWLIST 并注明原因。")
    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main())
