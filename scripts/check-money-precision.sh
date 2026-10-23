#!/usr/bin/env bash
# 金额精度守卫：禁止在 Java 业务代码里用浮点做金额换算/分摊。
# 例外：百分比/比率计算（变量名含 rate/ratio/percent）不在此列。
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root/server/src/main/java"

hits=0

# 1) 金额上下文里出现 double/float 除法或强制转换
while IFS= read -r line; do
  echo "FLOAT_MONEY: $line"
  hits=$((hits+1))
done < <(grep -rnE '(amount|price|fen|total|discount|refund|commission)[A-Za-z]*\s*/?\s*\(?\s*(double|float)\)|/ ?100\.0|/ ?100d' . --include=*.java \
         | grep -viE 'rate|ratio|percent|refill' \
         | grep -v 'money/Money\.java' || true)

# 2) String.format("%.2f", ... / 100)
while IFS= read -r line; do
  echo "FLOAT_FORMAT: $line"
  hits=$((hits+1))
done < <(grep -rn 'String.format("%\.2f"' . --include=*.java || true)

if [[ "$hits" -gt 0 ]]; then
  echo "金额精度守卫失败：$hits 处使用浮点。请改用 Money.toYuan / Money.mulDiv / BigDecimal。"
  exit 1
fi
echo "金额精度守卫通过：未发现浮点金额运算"
