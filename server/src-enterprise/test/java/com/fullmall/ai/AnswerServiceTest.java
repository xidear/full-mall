package com.fullmall.ai;

import com.fullmall.ai.app.AnswerService;
import com.fullmall.ai.app.ChatModel;
import com.fullmall.ai.app.KnowledgeChunk;
import com.fullmall.ai.app.KnowledgeRetriever;
import com.fullmall.ai.config.AiProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** AI 约束：无资料不猜、金额必须可追溯、越权承诺一律拒答。 */
class AnswerServiceTest {

    private final KnowledgeChunk product = new KnowledgeChunk(
            UUID.randomUUID(), "PRODUCT", "sku-1", "演示商品 · 规格与价格",
            "演示商品 500ml，售价 39.90 元，保质期 12 个月。", "v1");

    private AnswerService service(KnowledgeRetriever retriever, ChatModel model) {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setChatBaseUrl("http://localhost:8000/v1");
        properties.setChatModel("demo");
        properties.setEmbeddingBaseUrl("http://localhost:8000/v1");
        properties.setEmbeddingModel("demo-embed");
        properties.setTopK(5);
        return new AnswerService(retriever, model, properties);
    }

    @Test
    void noHitsMeansFallbackWithoutCallingModel() {
        boolean[] called = {false};
        AnswerService service = service(
                (query, topK) -> List.of(),
                (system, user) -> {
                    called[0] = true;
                    return "随便编一个答案";
                });

        var answer = service.ask("这个商品多少钱？");

        assertThat(called[0]).as("无资料时不得调用模型").isFalse();
        assertThat(answer.fallback()).isTrue();
        assertThat(answer.citations()).isEmpty();
        assertThat(answer.text()).contains("转人工");
    }

    @Test
    void hallucinatedPriceIsRejected() {
        AnswerService service = service(
                (query, topK) -> List.of(product),
                (system, user) -> "这个商品现在只要 19.90 元。");

        var answer = service.ask("这个商品多少钱？");

        assertThat(answer.fallback()).isTrue();
        assertThat(answer.violations()).anySatisfy(v -> assertThat(v).contains("金额无资料支撑"));
        assertThat(answer.text()).contains("转接");
    }

    @Test
    void forbiddenPromiseIsRejected() {
        AnswerService service = service(
                (query, topK) -> List.of(product),
                (system, user) -> "39.90 元，我保证一定能退，已为您退款。");

        var answer = service.ask("不合口味能退吗？");

        assertThat(answer.fallback()).isTrue();
        assertThat(answer.violations()).anySatisfy(v -> assertThat(v).contains("越权承诺"));
        assertThat(answer.violations()).anySatisfy(v -> assertThat(v).contains("声称已执行动作"));
    }

    @Test
    void groundedAnswerPassesWithCitations() {
        AnswerService service = service(
                (query, topK) -> List.of(product),
                (system, user) -> "售价 39.90 元，规格 500ml，保质期 12 个月。");

        var answer = service.ask("多少钱？");

        assertThat(answer.fallback()).isFalse();
        assertThat(answer.violations()).isEmpty();
        assertThat(answer.citations()).hasSize(1);
        assertThat(answer.citations().get(0).sourceId()).isEqualTo("sku-1");
    }
}
