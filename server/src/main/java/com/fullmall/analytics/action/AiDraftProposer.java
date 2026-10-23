package com.fullmall.analytics.action;

import com.fullmall.analytics.ai.OpenAiCompatClient;
import com.fullmall.platform.error.ApiException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 自然语言 → 受控草稿（N3）。
 *
 * <p>安全边界：
 * <ul>
 *   <li>模型只能产出**草稿**，仍需双人签署才会执行；</li>
 *   <li>动作必须在白名单内，参数缺失/类型不对一律拒绝；</li>
 *   <li>未配置模型时**明确报错**，不返回猜测结果。</li>
 * </ul>
 */
public class AiDraftProposer {

    private static final Set<String> ACTIONS = Set.of("sku.price.set", "product.status.set", "coupon.grant");

    private static final String SYSTEM_PROMPT = """
            你是电商后台的操作助手。把用户指令转成一个 JSON 对象，不要输出任何解释文字。
            JSON 结构：{"actionType":"...","payload":{...}}
            允许的 actionType 与 payload：
            - sku.price.set: {"skuCode":"字符串","priceAmount":整数分}
            - product.status.set: {"productId":"UUID","status":"ACTIVE|ARCHIVED|DRAFT"}
            - coupon.grant: {"memberId":"UUID","templateId":"UUID"}
            如果指令不明确或缺少必要参数，输出 {"actionType":"UNSUPPORTED","payload":{}}
            """;

    private final OpenAiCompatClient client;
    private final AiActionDraftService drafts;

    public AiDraftProposer(OpenAiCompatClient client, AiActionDraftService drafts) {
        this.client = client;
        this.drafts = drafts;
    }

    public boolean available() {
        return client != null;
    }

    public AiActionDraftService.Draft propose(String instruction, String requestedBy) {
        if (instruction == null || instruction.isBlank()) {
            throw ApiException.badRequest("AI_INSTRUCTION_REQUIRED", "指令不能为空");
        }
        if (client == null) {
            throw ApiException.conflict("AI_CAPABILITY_UNAVAILABLE",
                    "未配置模型（app.ai.*），无法自动生成草稿；可手工创建草稿");
        }
        String raw = client.chat(SYSTEM_PROMPT, instruction.trim());
        return drafts.createAllowingUnsupported(parse(raw), requestedBy);
    }

    /** 解析并校验模型输出；不合法就抛错，不做"尽力猜测"。 */
    Map.Entry<String, Map<String, Object>> parse(String raw) {
        var node = com.fullmall.platform.json.Json.read(raw);
        String actionType = node.path("actionType").asText("");
        if (!ACTIONS.contains(actionType)) {
            throw ApiException.badRequest("AI_ACTION_UNSUPPORTED",
                    "模型给出的动作不可执行：" + (actionType.isBlank() ? "(空)" : actionType));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        var payloadNode = node.path("payload");
        payloadNode.fieldNames().forEachRemaining(name ->
                payload.put(name, payloadNode.get(name).isNumber()
                        ? payloadNode.get(name).asLong() : payloadNode.get(name).asText()));
        return Map.entry(actionType, payload);
    }

    OpenAiCompatClient client() {
        return client;
    }
}
