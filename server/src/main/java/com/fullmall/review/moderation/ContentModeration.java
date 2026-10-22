package com.fullmall.review.moderation;

/**
 * 内容审核端口（UGC 统一入口）。
 *
 * <p>业务只依赖本接口；具体供应商（阿里云内容安全 / 腾讯云天御 / 自建）在独立包内，
 * 可整体删除（见 `docs/ADR-004_THIRD_PARTY_ISOLATION.md`）。
 */
public interface ContentModeration {

    /** 供应商标识（aliyun / tencent / local）。 */
    String provider();

    /**
     * 审核一段文本。
     *
     * <p>返回 `MANUAL` 表示需要人工复核（敏感词命中、供应商返回可疑、或未配置 API）。
     * 实现**不得**把"调用失败"当成"通过"。
     */
    Verdict review(String text);

    enum Decision { APPROVE, REJECT, MANUAL }

    record Verdict(Decision decision, String reason) {
        public static Verdict approve() {
            return new Verdict(Decision.APPROVE, "ok");
        }

        public static Verdict reject(String reason) {
            return new Verdict(Decision.REJECT, reason);
        }

        public static Verdict manual(String reason) {
            return new Verdict(Decision.MANUAL, reason);
        }
    }
}
