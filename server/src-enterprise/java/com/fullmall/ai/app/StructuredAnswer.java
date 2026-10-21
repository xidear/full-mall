package com.fullmall.ai.app;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 模型返回的结构化答案（JSON 契约）。
 *
 * <p>解析失败返回 `null`；调用方必须据此**拒绝使用模型输出**，而不是猜测其中的文本。
 */
public record StructuredAnswer(String answer, boolean grounded, boolean needsHuman,
                               java.util.List<Integer> citationIds) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static StructuredAnswer parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        // 容忍模型偶尔套 markdown 代码块（但内容仍必须是合法 JSON）
        if (text.startsWith("```")) {
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end > start) {
                text = text.substring(start, end + 1);
            }
        }
        try {
            var node = MAPPER.readTree(text);
            String answer = node.path("answer").asText("");
            if (answer.isBlank()) {
                return null;
            }
            var ids = new java.util.ArrayList<Integer>();
            for (var id : node.path("citationIds")) {
                if (id.isInt()) {
                    ids.add(id.asInt());
                }
            }
            return new StructuredAnswer(answer,
                    node.path("grounded").asBoolean(false),
                    node.path("needsHuman").asBoolean(true),
                    java.util.List.copyOf(ids));
        } catch (Exception error) {
            return null;
        }
    }
}
