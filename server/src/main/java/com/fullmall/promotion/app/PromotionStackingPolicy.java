package com.fullmall.promotion.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 促销叠加策略（纯函数）。
 *
 * <p>两种模式：
 * <ul>
 *   <li>`BEST_ONLY`（**默认，保持现有行为**）：只取优惠最大的那条；</li>
 *   <li>`STACK`：按优先级从高到低叠加，**同一互斥组只允许一条生效**（组内取优惠最大的）。</li>
 * </ul>
 *
 * <p>为什么需要互斥组：两个满减叠加可能导致 0 元购；用"组"表达互斥比"两两配对"更可维护。
 */
public final class PromotionStackingPolicy {

    private PromotionStackingPolicy() {
    }

    public enum Mode { BEST_ONLY, STACK }

    /** 候选规则；`exclusiveGroup` 为空表示不参与互斥。 */
    public record Candidate(String code, long discountAmount, int priority, String exclusiveGroup) {
    }

    public record Applied(List<Candidate> applied, long totalDiscount) {
    }

    public static Applied apply(Mode mode, List<Candidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new Applied(List.of(), 0);
        }
        if (mode == Mode.BEST_ONLY) {
            Candidate best = candidates.stream()
                    .max(Comparator.comparingLong(Candidate::discountAmount))
                    .orElseThrow();
            return new Applied(List.of(best), best.discountAmount());
        }
        List<Candidate> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingInt(Candidate::priority).reversed()
                .thenComparing(Comparator.comparingLong(Candidate::discountAmount).reversed()));

        List<Candidate> applied = new ArrayList<>();
        java.util.Set<String> usedGroups = new java.util.HashSet<>();
        long total = 0;
        for (Candidate candidate : sorted) {
            String group = candidate.exclusiveGroup();
            if (group != null && !group.isBlank()) {
                if (!usedGroups.add(group)) {
                    continue;   // 同组已有更优者生效
                }
            }
            applied.add(candidate);
            total += candidate.discountAmount();
        }
        return new Applied(List.copyOf(applied), total);
    }
}
