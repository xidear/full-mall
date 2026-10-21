package com.fullmall.ai.app;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 演示模式对话模型：**抽取式回答**，不调用任何外部服务。
 *
 * <p>作用：没有 GPU/API 时也能演示"资料 → 检索 → 回答 + 引用"的完整链路。
 * 它的输出**完全来自资料原文**，因此天然合规；代价是不会改写、不会寒暄，
 * 这一点在演示时必须说明，不能包装成"模型很聪明"。
 */
@Component
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "demo")
public class DemoChatModel implements ChatModel {

    private static final Pattern QUESTION = Pattern.compile("【问题】\\s*(.+)", Pattern.DOTALL);
    private static final Pattern FIRST_PARA = Pattern.compile("\\[(\\d+)]\\s.*?\\n(.*?)(?:\\n\\n|$)", Pattern.DOTALL);

    @Override
    public String complete(String system, String user) {
        Matcher question = QUESTION.matcher(user == null ? "" : user);
        String asked = question.find() ? question.group(1).trim() : "";

        Matcher first = FIRST_PARA.matcher(user == null ? "" : user);
        if (!first.find()) {
            return "没有可用资料，建议转人工。";
        }
        String content = first.group(2).replaceAll("\\s+", " ").trim();
        if (content.length() > 200) {
            content = content.substring(0, 200) + "……";
        }
        return "根据资料：" + content;
    }
}
