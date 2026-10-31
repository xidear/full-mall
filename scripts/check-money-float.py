#!/usr/bin/env python3
"""金额相关代码禁止 double/float/Math.round（AGENTS §1 / 用户要求 bcadd 式精确计算）。

只扫描金额核心文件；注释里的“禁止 double”不算违规。
"""
import re
import sys
from pathlib import Path

FILES = [
    "server/src/main/java/com/fullmall/platform/money/Money.java",
    "server/src/main/java/com/fullmall/platform/money/CurrencyPrecision.java",
    "server/src/main/java/com/fullmall/exchange/app/ExchangeRateService.java",
    "server/src/main/java/com/fullmall/exchange/app/ExchangeRateFetchService.java",
    "server/src/main/java/com/fullmall/pos/domain/PosCalculator.java",
    "server/src/main/java/com/fullmall/promotion/app/BargainPricing.java",
    "web/packages/api-client/src/money.ts",
]
PATTERN = re.compile(r"\b(?:double|float)\b|Math\.round\(")


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    text = re.sub(r"//.*", "", text)
    return text


def main() -> int:
    root = Path(__file__).resolve().parent.parent
    failures = []
    for relative in FILES:
        path = root / relative
        if not path.exists():
            failures.append(f"{relative}: 文件不存在")
            continue
        for number, line in enumerate(strip_comments(path.read_text(encoding="utf-8")).splitlines(), 1):
            if PATTERN.search(line):
                failures.append(f"{relative}:{number}: {line.strip()}")
    if failures:
        print("金额代码发现浮点/Math.round（必须用 BigDecimal 或整数）：")
        for item in failures:
            print("  " + item)
        return 1
    print(f"金额浮点检查通过（{len(FILES)} 个核心文件）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
