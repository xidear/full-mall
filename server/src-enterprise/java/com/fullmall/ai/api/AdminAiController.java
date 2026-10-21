package com.fullmall.ai.api;

import com.fullmall.ai.config.AiProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 模块状态接口（企业版专有）。
 *
 * <p>社区版打包时该类根本不存在，因此 `/admin/v1/ai/**` 在社区版返回 404 —— 这是验收项 AI-1。
 */
@RestController
@RequestMapping("/admin/v1/ai")
public class AdminAiController {

    private final AiProperties properties;

    public AdminAiController(AiProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("edition", "enterprise");
        body.put("enabled", properties.isEnabled());
        body.put("ready", properties.isReady());
        if (!properties.isEnabled()) {
            body.put("reason", "AI 模块未启用（app.ai.enabled=false）");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
        if (!properties.isReady()) {
            // 明确指出缺哪一项，而不是笼统说"不可用"
            var missing = new java.util.ArrayList<String>();
            if (properties.getChatBaseUrl() == null || properties.getChatBaseUrl().isBlank()) {
                missing.add("app.ai.chat-base-url");
            }
            if (properties.getChatModel() == null || properties.getChatModel().isBlank()) {
                missing.add("app.ai.chat-model");
            }
            if (properties.getEmbeddingBaseUrl() == null || properties.getEmbeddingBaseUrl().isBlank()) {
                missing.add("app.ai.embedding-base-url");
            }
            if (properties.getEmbeddingModel() == null || properties.getEmbeddingModel().isBlank()) {
                missing.add("app.ai.embedding-model");
            }
            body.put("missing", missing);
            body.put("reason", "AI 模型未配置：" + String.join(", ", missing));
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
        body.put("chatModel", properties.getChatModel());
        body.put("embeddingModel", properties.getEmbeddingModel());
        body.put("topK", properties.getTopK());
        return ResponseEntity.ok(body);
    }
}
