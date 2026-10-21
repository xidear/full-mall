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
    private final com.fullmall.ai.app.AnswerService answerService;
    private final com.fullmall.ai.app.DocumentIngestionService ingestion;
    private final com.fullmall.ai.app.KnowledgeStore knowledgeStore;
    private final com.fullmall.ai.app.ProductKnowledgeSyncService productSync;

    public AdminAiController(AiProperties properties,
                             com.fullmall.ai.app.AnswerService answerService,
                             com.fullmall.ai.app.DocumentIngestionService ingestion,
                             com.fullmall.ai.app.KnowledgeStore knowledgeStore,
                             com.fullmall.ai.app.ProductKnowledgeSyncService productSync) {
        this.properties = properties;
        this.answerService = answerService;
        this.ingestion = ingestion;
        this.knowledgeStore = knowledgeStore;
        this.productSync = productSync;
    }

    public record AskRequest(String question) {
    }

    /**
     * 问答入口（企业版）。未启用或未配置模型时返回 503，由客服台明确提示"AI 不可用"。
     */
    @org.springframework.web.bind.annotation.PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(@org.springframework.web.bind.annotation.RequestBody AskRequest body) {
        ResponseEntity<Map<String, Object>> notReady = readiness();
        if (notReady != null) {
            return notReady;
        }
        var answer = answerService.ask(body == null ? null : body.question());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("answer", answer.text());
        result.put("fallback", answer.fallback());
        result.put("violations", answer.violations());
        result.put("citations", answer.citations().stream()
                .map(chunk -> Map.of(
                        "sourceType", chunk.sourceType(),
                        "sourceId", chunk.sourceId(),
                        "title", chunk.title()))
                .toList());
        return ResponseEntity.ok(result);
    }

    /** 客户资料导入（企业版）：multipart 上传，返回切块统计；失败按类型明确报错。 */
    @org.springframework.web.bind.annotation.PostMapping(
            value = "/knowledge/import", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importKnowledge(
            @org.springframework.web.bind.annotation.RequestParam("file")
            org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        ResponseEntity<Map<String, Object>> notReady = readiness();
        if (notReady != null) {
            return notReady;
        }
        if (file == null || file.isEmpty()) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "EMPTY_DOCUMENT", "上传文件为空");
        }
        var result = ingestion.importFile(file.getOriginalFilename(), file.getBytes());
        return ResponseEntity.ok(Map.of(
                "sourceId", result.sourceId(),
                "type", result.detectedType(),
                "chunks", result.chunks(),
                "chars", result.chars(),
                "totalChunks", knowledgeStore.size()));
    }

    /** 商品知识同步：改价/改描述后调用，立即重建该 SKU 的知识块。 */
    @org.springframework.web.bind.annotation.PostMapping("/knowledge/sync")
    public ResponseEntity<Map<String, Object>> syncProduct(
            @org.springframework.web.bind.annotation.RequestParam java.util.UUID skuId) {
        ResponseEntity<Map<String, Object>> notReady = readiness();
        if (notReady != null) {
            return notReady;
        }
        var result = productSync.sync(skuId);
        return ResponseEntity.ok(Map.of(
                "skuId", result.skuId(),
                "title", result.title(),
                "chunks", result.chunks(),
                "totalChunks", knowledgeStore.size()));
    }

    private ResponseEntity<Map<String, Object>> readiness() {
        if (properties.isReady()) {
            return null;
        }
        return status();
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
