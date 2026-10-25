#!/usr/bin/env python3
"""检索评测：用关键词召回（与生产 keyword 模式同口径）计算 Top-K 命中率。

用法：
    python3 scripts/eval-retrieval.py --base http://localhost:41280 \
        --token <admin-token> [--topk 3]

数据来源：docs/ai/eval-set.json（50 条标注问题）+ 已导入知识库（商品/文档）。
判定：Top-K 结果中任一 `title` 或 `content` 含 expect 关键词即算命中。
"""
import argparse
import json
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EVAL = ROOT / "docs" / "ai" / "eval-set.json"


def search(base, token, question, topk):
    url = f"{base}/admin/v1/ai/knowledge/search?q={urllib.parse.quote(question)}&topK={topk}"
    request = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    with urllib.request.urlopen(request, timeout=15) as response:
        return json.loads(response.read().decode())


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="http://localhost:41280")
    parser.add_argument("--token", required=True)
    parser.add_argument("--topk", type=int, default=3)
    parser.add_argument("--threshold", type=float, default=0.8)
    args = parser.parse_args()

    cases = json.loads(EVAL.read_text(encoding="utf-8"))["cases"]
    hits = 0
    misses = []
    for case in cases:
        try:
            rows = search(args.base, args.token, case["q"], args.topk)
        except Exception as error:  # 查询失败必须显式记录，不能当未命中
            misses.append((case["q"], f"ERROR: {error}"))
            continue
        hit = any(case["expect"] in (row.get("title") or "") + (row.get("content") or "")
                  for row in rows)
        if hit:
            hits += 1
        else:
            misses.append((case["q"], [row.get("title") for row in rows]))

    rate = hits / len(cases)
    print(f"Top-{args.topk} 命中率: {rate:.1%} ({hits}/{len(cases)})")
    if misses:
        print("未命中：")
        for question, got in misses[:20]:
            print(f"  - {question} → {got}")
    return 0 if rate >= args.threshold else 1


if __name__ == "__main__":
    sys.exit(main())
