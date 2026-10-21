package com.fullmall.ai;

import com.fullmall.ai.app.AnswerService;
import com.fullmall.ai.app.DemoChatModel;
import com.fullmall.ai.app.DocumentIngestionService;
import com.fullmall.ai.app.InMemoryKnowledgeStore;
import com.fullmall.ai.config.AiProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 资料导入 + 演示模式问答：解析、切分、检索、约束一条链路。 */
class IngestionAndDemoTest {

    private AnswerService service(InMemoryKnowledgeStore store) {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setMode("demo");
        properties.setTopK(5);
        var ingestion = new DocumentIngestionService(store);
        return new AnswerService((q, topK) -> store.search(q, topK),
                new DemoChatModel(), properties);
    }

    @Test
    void markdownQaIsParsedChunkedAndAnswerable() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        var ingestion = new DocumentIngestionService(store);
        String md = """
                Q: 鲜奶可以退货吗？
                A: 鲜奶属于生鲜商品，未开封且签收 24 小时内可申请退货。

                Q: 多久发货？
                A: 现货商品在付款后 24 小时内发出。
                """;

        var result = ingestion.ingest("faq.md", md.getBytes(StandardCharsets.UTF_8));

        assertThat(result.chunks()).isEqualTo(2);
        assertThat(store.size()).isEqualTo(2);

        var answer = service(store).ask("鲜奶可以退货吗");
        assertThat(answer.fallback()).isFalse();
        assertThat(answer.text()).contains("未开封");
        assertThat(answer.citations()).hasSize(1);
        assertThat(answer.citations().get(0).title()).contains("faq.md");
    }

    @Test
    void unsupportedBinaryIsRejectedNotStored() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        var ingestion = new DocumentIngestionService(store);

        assertThatThrownBy(() -> ingestion.ingest("photo.png", new byte[]{(byte) 0x89, 'P', 'N', 'G'}))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不支持的文件类型");
        assertThat(store.size()).isZero();
    }

    @Test
    void emptyDocumentIsRejected() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        var ingestion = new DocumentIngestionService(store);

        assertThatThrownBy(() -> ingestion.ingest("empty.txt", "   \n\n  ".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("解析后没有文字");
    }

    @Test
    void unanswerableQuestionFallsBackWithoutModel() {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        new DocumentIngestionService(store).ingest("faq.md",
                "Q: 多久发货？\nA: 24 小时内发出。\n".getBytes(StandardCharsets.UTF_8));

        var answer = service(store).ask("支持开发票吗");

        assertThat(answer.fallback()).isTrue();
        assertThat(answer.citations()).isEmpty();
    }
}
