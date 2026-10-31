package com.fullmall.ai.answer;

import com.fullmall.ai.knowledge.SemanticIndexService;
import com.fullmall.ai.media.AiMediaService;
import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AI 问答三态：空问题 400 / 未配模型 503 / 可用时 200（命中带引用，未命中拒答）。
 *
 * <p>核心回归点：**检索不到必须拒答**，不能编答案；未配模型必须是 503 而不是"空回答"。
 */
class AiAnswerServiceTest {

    private static AiAnswerService service(List<String> textProviders, boolean semanticEnabled,
                                           List<Map<String, Object>> hits, ApiException searchError) {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), eq(String.class))).thenReturn(textProviders);

        AiMediaService media = mock(AiMediaService.class);
        when(media.chat(anyString(), anyString(), anyString()))
                .thenReturn("满 99 元免运费，偏远地区另计。");

        SemanticIndexService index = mock(SemanticIndexService.class);
        when(index.enabled()).thenReturn(semanticEnabled);
        if (searchError != null) {
            when(index.search(anyString(), anyString(), anyInt(), anyString())).thenThrow(searchError);
        } else {
            when(index.search(anyString(), anyString(), anyInt(), anyString())).thenReturn(hits);
        }

        return new AiAnswerService(jdbc, media, index, mock(AuditEventService.class), 0.72, 5, "");
    }

    @Test
    void blankQuestionIsRejected() {
        var svc = service(List.of("ollama"), true, List.of(), null);

        assertThatThrownBy(() -> svc.ask("   ", "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("问题不能为空");
    }

    @Test
    void missingProviderIsServiceUnavailable() {
        var svc = service(List.of(), true, List.of(), null);

        assertThatThrownBy(() -> svc.ask("运费怎么算", "admin"))
                .isInstanceOf(ApiException.class)
                .satisfies(err -> assertThat(((ApiException) err).code())
                        .isEqualTo("AI_PROVIDER_NOT_CONFIGURED"));
    }

    @Test
    void semanticDisabledIsServiceUnavailable() {
        var svc = service(List.of("ollama"), false, List.of(), null);

        assertThatThrownBy(() -> svc.ask("运费怎么算", "admin"))
                .isInstanceOf(ApiException.class)
                .satisfies(err -> assertThat(((ApiException) err).code())
                        .isEqualTo("AI_SEMANTIC_DISABLED"));
    }

    @Test
    void hitAboveThresholdAnswersWithCitations() {
        var svc = service(List.of("ollama"), true,
                List.of(Map.of("sourceId", "faq-shipping", "content", "满 99 免运费", "score", 0.91)), null);

        var answer = svc.ask("运费怎么算", "admin");

        assertThat(answer.fallback()).isFalse();
        assertThat(answer.answer()).contains("免运费");
        assertThat(answer.citations()).hasSize(1);
        assertThat(answer.citations().get(0).sourceId()).isEqualTo("faq-shipping");
    }

    @Test
    void lowScoreRefusesInsteadOfGuessing() {
        var svc = service(List.of("ollama"), true,
                List.of(Map.of("sourceId", "faq-other", "content", "无关内容", "score", 0.31)), null);

        var answer = svc.ask("运费怎么算", "admin");

        assertThat(answer.fallback()).isTrue();
        assertThat(answer.answer()).isNull();
        assertThat(answer.reason()).contains("阈值");
    }

    @Test
    void bannedQuestionIsRefusedWithFixedReply() {
        var svc = service(List.of("ollama"), true, List.of(), null);

        var answer = svc.ask("你这个傻逼机器人", "admin");

        assertThat(answer.fallback()).isTrue();
        assertThat(answer.answer()).isNull();
        assertThat(answer.reason()).contains("不当内容");
    }

    @Test
    void missingEmbeddingIsServiceUnavailable() {
        var svc = service(List.of("ollama"), true, List.of(),
                ApiException.conflict("AI_EMBEDDING_UNAVAILABLE", "没有可用的 embedding 供应商"));

        assertThatThrownBy(() -> svc.ask("运费怎么算", "admin"))
                .isInstanceOf(ApiException.class)
                .satisfies(err -> assertThat(((ApiException) err).code())
                        .isEqualTo("AI_EMBEDDING_NOT_CONFIGURED"));
    }

    @Test
    void emptyIndexRefusesInsteadOfFailing() {
        var svc = service(List.of("ollama"), true, List.of(),
                ApiException.notFound("SEMANTIC_INDEX_EMPTY", "索引为空"));

        var answer = svc.ask("运费怎么算", "admin");

        assertThat(answer.fallback()).isTrue();
        assertThat(answer.reason()).contains("知识库为空");
    }
}
