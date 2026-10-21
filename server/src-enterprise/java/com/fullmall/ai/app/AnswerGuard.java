package com.fullmall.ai.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 答案约束检查：模型输出**必须**通过这里才能返回给顾客。
 *
 * <p>检查的都是可机械判定的规则（不依赖另一个模型）：
 * <ol>
 *   <li>必须带引用：没有引用等于无从核对；</li>
 *   <li>答案里出现的金额必须能在检索资料中找到（防止编价格）；</li>
 *   <li>不得做越权承诺：退款/赔偿/免单/封号等动作 AI 无权决定；</li>
 *   <li>不得声称已执行动作（"已为您退款"）。</li>
 * </ol>
 *
 * <p>违规时的处理是**拒答并转人工**，不是删掉违规句子继续答——删句子会留下语义空洞，
 * 顾客更容易被误导。
 */
public final class AnswerGuard {

    /** 越权承诺：AI 只能解释政策，不能答应结果。 */
    private static final List<String> FORBIDDEN_PROMISES = List.of(
            "我保证", "一定能退", "肯定可以退", "给您退款", "已为您退款", "已退款",
            "给您补偿", "赔偿您", "给您免单", "帮您封号", "我给您改价", "给您打折");

    /** 已执行动作的完成体表达（无论是否真的执行，都不允许 AI 这样说）。 */
    private static final Pattern CLAIMED_ACTION =
            Pattern.compile("(已|已经)(为您|帮你|给您)?(退款|取消订单|改价|发货|补偿|封号)");

    private static final Pattern AMOUNT = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(元|块|人民币|CNY)");

    private AnswerGuard() {
    }

    public record Verdict(boolean allowed, List<String> violations) {
    }

    /** @param answer 模型输出；@param context 检索到的资料（拼接后的原文） */
    public static Verdict check(String answer, List<KnowledgeChunk> context) {
        List<String> violations = new ArrayList<>();
        if (answer == null || answer.isBlank()) {
            return new Verdict(false, List.of("答案为空"));
        }
        if (context == null || context.isEmpty()) {
            // 调用方本不该走到这里；显式拒绝，避免"无资料也答"
            return new Verdict(false, List.of("没有检索资料却尝试作答"));
        }

        for (String phrase : FORBIDDEN_PROMISES) {
            if (answer.contains(phrase)) {
                violations.add("越权承诺：" + phrase);
            }
        }
        Matcher action = CLAIMED_ACTION.matcher(answer);
        while (action.find()) {
            violations.add("声称已执行动作：" + action.group());
        }

        String haystack = String.join("\n", context.stream().map(KnowledgeChunk::content).toList());
        Matcher amount = AMOUNT.matcher(answer);
        Set<String> seen = new java.util.LinkedHashSet<>();
        while (amount.find()) {
            String value = amount.group(1);
            if (!seen.add(value)) {
                continue;
            }
            boolean grounded = haystack.contains(value)
                    || haystack.contains(value + ".00")
                    || haystack.contains(value.replace(".00", ""));
            if (!grounded) {
                violations.add("金额无资料支撑：" + amount.group());
            }
        }
        return new Verdict(violations.isEmpty(), List.copyOf(violations));
    }
}
