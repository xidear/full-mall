#!/usr/bin/env bash
# 关键文件守卫：远端被强推回退时，先发现"文件消失"而不是等测试报错。
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
missing=0

files=(
  server/src/main/java/com/fullmall/payment/wechat/WechatV3Signer.java
  server/src/main/java/com/fullmall/payment/wechat/WechatNativeOrderClient.java
  server/src/main/java/com/fullmall/payment/wechat/WechatRefundClient.java
  server/src/main/java/com/fullmall/payment/wechat/WechatBillClient.java
  server/src/main/java/com/fullmall/payment/wechat/WechatTransferClient.java
  server/src/main/java/com/fullmall/payment/alipay/AlipaySigner.java
  server/src/main/java/com/fullmall/payment/alipay/AlipayPrecreateClient.java
  server/src/main/java/com/fullmall/payment/alipay/AlipayRefundClient.java
  server/src/main/java/com/fullmall/payment/alipay/AlipayCallbackVerifier.java
  server/src/main/java/com/fullmall/payment/callback/WechatV3CallbackVerifier.java
  server/src/main/java/com/fullmall/payment/reconcile/PaymentReconciliationService.java
  server/src/main/java/com/fullmall/catalog/app/ProductCsvImporter.java
  server/src/main/java/com/fullmall/catalog/app/SkuOptionsProjectionService.java
  server/src/main/java/com/fullmall/coupon/app/CouponAllocationService.java
  server/src/main/java/com/fullmall/supplier/webhook/WebhookDispatcher.java
  server/src/main/java/com/fullmall/i18n/UiTranslationService.java
  server/src/main/java/com/fullmall/analytics/tool/AiReadOnlyTools.java
  server/src/main/java/com/fullmall/analytics/ai/OpenAiCompatClient.java
  server/src/main/java/com/fullmall/ai/media/AiMediaService.java
  server/src/main/java/com/fullmall/ai/media/AiMediaWorkerTask.java
  server/src/main/java/com/fullmall/ai/media/AiProviderBootstrap.java
  server/src/main/java/com/fullmall/ai/media/adapter/OpenAiImagesAdapter.java
  server/src/main/java/com/fullmall/ai/media/adapter/DashScopeImageAdapter.java
  server/src/main/java/com/fullmall/ai/media/adapter/DashScopeVideoAdapter.java
  server/src/main/java/com/fullmall/ai/media/adapter/OpenAiEmbeddingAdapter.java
  server/src/main/java/com/fullmall/catalog/app/ProductMediaService.java
  server/src/main/java/com/fullmall/review/moderation/http/HttpModerationAdapter.java
  server/src/main/java/com/fullmall/platform/asset/cos/CosAssetStorage.java
  server/src/main/java/com/fullmall/platform/asset/qiniu/QiniuAssetStorage.java
  server/src/main/java/com/fullmall/distribution/app/LaborIncomeTax.java
  server/src/main/java/com/fullmall/distribution/app/PayoutGateway.java
)

for f in "${files[@]}"; do
  if [[ ! -f "$root/$f" ]]; then
    echo "MISSING: $f"
    missing=$((missing + 1))
  fi
done

if [[ "$missing" -gt 0 ]]; then
  echo "关键文件缺失 $missing 个（可能被远端强推回退）"
  exit 1
fi
echo "关键文件齐全（${#files[@]} 个）"
