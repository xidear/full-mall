package com.fullmall.catalog.app;

import com.fullmall.platform.error.ApiException;

import java.util.EnumSet;
import java.util.Set;

/**
 * 翻译状态机（I18N-003/004）。
 *
 * <p>允许的流转：
 * <pre>
 * DRAFT  → MACHINE | REVIEWED
 * MACHINE → REVIEWED | OUTDATED
 * REVIEWED → PUBLISHED | OUTDATED
 * PUBLISHED → OUTDATED
 * OUTDATED → REVIEWED（重新审校后可再发布）
 * </pre>
 *
 * <p>硬规则：**只有 REVIEWED 可以发布**；源文变更后一律 OUTDATED，
 * 法律/支付类文案未审校不得发布（由调用方按 `sensitive` 判定）。
 */
public enum TranslationStatus {

    DRAFT,
    MACHINE,
    REVIEWED,
    PUBLISHED,
    OUTDATED;

    private static final Set<TranslationStatus> ALLOWED_FROM_PUBLISH = EnumSet.of(REVIEWED);

    public boolean canTransitionTo(TranslationStatus target) {
        return switch (this) {
            case DRAFT -> target == MACHINE || target == REVIEWED;
            case MACHINE -> target == REVIEWED || target == OUTDATED;
            case REVIEWED -> target == PUBLISHED || target == OUTDATED;
            case PUBLISHED -> target == OUTDATED;
            case OUTDATED -> target == REVIEWED;
        };
    }

    /**
     * 发布门禁：敏感文案（法律/支付/隐私）必须已审校。
     *
     * @throws ApiException 状态不允许发布时抛出，明确失败而不是静默降级
     */
    public void requirePublishable(boolean sensitive) {
        if (!ALLOWED_FROM_PUBLISH.contains(this)) {
            throw ApiException.conflict("TRANSLATION_NOT_REVIEWED",
                    "只有已审校的译文可以发布，当前状态=" + this);
        }
        if (sensitive) {
            // REVIEWED 即代表人工审校完成；此处保留分支以便将来区分"机审/人审"
            return;
        }
    }
}