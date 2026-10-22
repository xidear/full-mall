package com.fullmall.review.app;

import com.fullmall.review.moderation.ContentModeration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 评价审核策略：**先本地过滤，再决定自动通过 / 人工**。
 *
 * <p>规则（按业务要求）：
 * <ol>
 *   <li>命中本地敏感词 → 直接 `REJECT`（不浪费供应商调用）；</li>
 *   <li>未配置审核供应商（`app.review.moderation.provider` 非 aliyun/tencent 等）→ **全部人工**；</li>
 *   <li>已配置 → 调供应商：通过则 `PUBLISHED`，可疑/失败则 `PENDING`（人工）；</li>
 *   <li>供应商调用异常**绝不**当成通过，一律转人工。</li>
 * </ol>
 */
@Service
public class ReviewModerationPolicy {

    private static final Set<String> DEFAULT_BLOCKED = Set.of(
            "赌博", "博彩", "毒品", "枪支", "代开发票", "刷单", "假货", "诈骗", "色情");

    private final ObjectProvider<ContentModeration> moderation;
    private final boolean autoModerationEnabled;
    private final Set<String> blockedWords;

    public ReviewModerationPolicy(
            ObjectProvider<ContentModeration> moderation,
            @Value("${app.review.moderation.auto:true}") boolean autoModerationEnabled,
            @Value("${app.review.moderation.blocked-words:}") String extraBlocked) {
        this.moderation = moderation;
        this.autoModerationEnabled = autoModerationEnabled;
        Set<String> words = new java.util.LinkedHashSet<>(DEFAULT_BLOCKED);
        if (extraBlocked != null && !extraBlocked.isBlank()) {
            for (String word : extraBlocked.split(",")) {
                if (!word.isBlank()) {
                    words.add(word.trim());
                }
            }
        }
        this.blockedWords = Set.copyOf(words);
    }

    public enum Outcome { PUBLISH, PENDING, REJECT }

    public record Result(Outcome outcome, String reason) {
    }

    public Result decide(String content) {
        if (content == null || content.isBlank()) {
            return new Result(Outcome.PUBLISH, "无正文");
        }
        String lower = content.toLowerCase(Locale.ROOT);
        List<String> hits = blockedWords.stream().filter(lower::contains).toList();
        if (!hits.isEmpty()) {
            return new Result(Outcome.REJECT, "命中敏感词：" + String.join(",", hits));
        }
        if (!autoModerationEnabled) {
            return new Result(Outcome.PENDING, "自动审核已关闭，转人工");
        }
        ContentModeration provider = moderation.getIfAvailable();
        if (provider == null) {
            return new Result(Outcome.PENDING, "未配置审核供应商，转人工");
        }
        try {
            var verdict = provider.review(content);
            return switch (verdict.decision()) {
                case APPROVE -> new Result(Outcome.PUBLISH, provider.provider() + ":ok");
                case REJECT -> new Result(Outcome.REJECT, verdict.reason());
                case MANUAL -> new Result(Outcome.PENDING, verdict.reason());
            };
        } catch (RuntimeException error) {
            // 供应商挂掉不能放行
            return new Result(Outcome.PENDING, "审核服务异常，转人工：" + error.getMessage());
        }
    }
}
