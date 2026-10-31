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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AI 问答链路：空问题 400 / 未配模型 503 / 检索不到拒答 / 命中后由真实模型组织答案并带引用。
 *
 * <p>核心回归点：**检索不到必须拒答**，不能编答案；模型不可达必须是 503，不能伪装成"没有答案"。
 */
class AiAnswerServiceTest {

    private static AiAnswerService service(String retrieval, List<String> chatProviders,
                                           boolean semanticEnabled,
                                           List<Map<String, Object>> semanticHits,
                                           ApiException searchError,
                                           List<KeywordRetriever.Hit> keywordHits,
                                           String chatReply) {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), eq(String.class))).thenReturn(chatProviders);

        AiMediaService media = mock(AiMediaService.class);
        when(media.chat(anyString(), anyString(), anyString())).thenReturn(chatReply);

        SemanticIndexService index = mock(SemanticIndexService.class);
        when(index.enabled()).thenReturn(semanticEnabled);
        if (searchError != null) {
            when(index.search(anyString(), anyString(), anyInt(), nullable(String.class)))
                    .thenThrow(searchError);
        } else {
            when(index.search(anyString(), anyString(), anyInt(), nullable(String.class)))
                    .thenReturn(semanticHits);
        }

        KeywordRetriever keyword = mock(KeywordRetriever.class);
        when(keyword.search(anyString(), nullable(String.class), anyInt())).thenReturn(keywordHits);

        return new AiAnswerService(jdbc, media, index, keyword, mock(AuditEventService.class),
                retrieval, 0.72, 0.15, 5, "");
    }

    private static AiAnswerService keywordService(List<KeywordRetriever.Hit> hits, String chatReply) {
        return service("KEYWORD", List.of("company-chat"), true, List.of(), null, hits, chatReply);
    }

    @Test
    void blankQuestionIsRejected() {
        var svc = service("KEYWORD", List.of("company-chat"), true, List.of(), null, List.of(), "答案");

        assertThatThrownBy(() -> svc.ask("   ", "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("问题不能为空");
    }

    @Test
    void missingChatProviderIsServiceUnavailable() {
        var svc = service("KEYWORD", List.of(), true, List.of(), null, List.of(), "答案");

        assertThatThrownBy(() -> svc.ask("运费怎么算", "admin"))
                .isInstanceOf(ApiException.class)
                .satisfies(err -> assertThat(((ApiException) err).code())
                        .isEqualTo("AI_CHAT_NOT_CONFIGURED"));
    }

    @Test
    void semanticDisabledIsServiceUnavailable() {
        var svc = service("SEMANTIC", List.of("company-chat"), false, List.of(), null, List.of(), "答案");

        assertThatThrownBy(() -> svc.ask("运费怎么算", "admin"))
                .isInstanceOf(ApiException.class)
                .satisfies(err -> assertThat(((ApiException) err).code())
                        .isEqualTo("AI_SEMANTIC_DISABLED"));
    }

    @Test
    void keywordHitIsAnsweredByModelWithCitations() {
        var svc = keywordService(List.of(new KeywordRetriever.Hit(
                "faq-shipping", "运费怎么算", "满 99 元免运费，偏远地区另计。", 0.9)),
                "满 99 元免运费，偏远地区另计。");

        var answer = svc.ask("运费怎么算", "admin");

        assertThat(answer.fallback()).isFalse();
        assertThat(answer.answer()).contains("免运费");
        assertThat(answer.citations()).hasSize(1);
        assertThat(answer.citations().get(0).sourceId()).isEqualTo("faq-shipping");
        assertThat(answer.provider()).isEqualTo("company-chat");
        assertThat(answer.retrieval()).isEqualTo("KEYWORD");
    }

    @Test
    void lowKeywordScoreRefusesInsteadOfGuessing() {
        var svc = keywordService(List.of(new KeywordRetriever.Hit(
                "faq-other", "无关", "无关内容", 0.05)), "不该被调用");

        var answer = svc.ask("运费怎么算", "admin");

        assertThat(answer.fallback()).isTrue();
        assertThat(answer.answer()).isNull();
        assertThat(answer.reason()).contains("阈值");
    }

    @Test
    void semanticHitAboveThresholdAnswersWithCitations() {
        var svc = service("SEMANTIC", List.of("company-chat"), true,
                List.of(Map.of("sourceId", "faq-shipping", "content", "满 99 免运费", "score", 0.91)),
                null, List.of(), "满 99 元免运费。");

        var answer = svc.ask("运费怎么算", "admin");

        assertThat(answer.fallback()).isFalse();
        assertThat(answer.citations()).hasSize(1);
        assertThat(answer.retrieval()).isEqualTo("SEMANTIC");
    }

    @Test
    void bannedQuestionIsRefusedWithFixedReply() {
        var svc = keywordService(List.of(), "不该被调用");

        var answer = svc.ask("你这个傻逼机器人", "admin");

        assertThat(answer.fallback()).isTrue();
        assertThat(answer.answer()).isNull();
        assertThat(answer.reason()).contains("不当内容");
    }

    @Test
    void missingEmbeddingIsServiceUnavailable() {
        var svc = service("SEMANTIC", List.of("company-chat"), true, List.of(),
                ApiException.conflict("AI_EMBEDDING_UNAVAILABLE", "没有可用的 embedding 供应商"),
                List.of(), "答案");

        assertThatThrownBy(() -> svc.ask("运费怎么算", "admin"))
                .isInstanceOf(ApiException.class)
                .satisfies(err -> assertThat(((ApiException) err).code())
                        .isEqualTo("AI_EMBEDDING_NOT_CONFIGURED"));
    }

    @Test
    void emptyIndexRefusesInsteadOfFailing() {
        var svc = service("SEMANTIC", List.of("company-chat"), true, List.of(),
                ApiException.notFound("SEMANTIC_INDEX_EMPTY", "索引为空"), List.of(), "答案");

        var answer = svc.ask("运费怎么算", "admin");

        assertThat(answer.fallback()).isTrue();
        assertThat(answer.reason()).contains("阈值");
    }

    @Test
    void modelCannotAnswerFallsBack() {
        var svc = keywordService(List.of(new KeywordRetriever.Hit(
                "faq-1", "退货", "7 天无理由", 0.8)), "无法回答");

        var answer = svc.ask("能退货吗", "admin");

        assertThat(answer.fallback()).isTrue();
        assertThat(answer.reason()).contains("片段不足");
        assertThat(answer.citations()).hasSize(1);
    }

    @Test
    void modelFailureIsServiceUnavailable() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), eq(String.class))).thenReturn(List.of("company-chat"));
        AiMediaService media = mock(AiMediaService.class);
        when(media.chat(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("connect timed out"));
        SemanticIndexService index = mock(SemanticIndexService.class);
        KeywordRetriever keyword = mock(KeywordRetriever.class);
        when(keyword.search(anyString(), nullable(String.class), anyInt()))
                .thenReturn(List.of(new KeywordRetriever.Hit("faq-1", "运费", "满 99 免运费", 0.9)));

        var svc = new AiAnswerService(jdbc, media, index, keyword,
                mock(AuditEventService.class), "KEYWORD", 0.72, 0.15, 5, "");

        assertThatThrownBy(() -> svc.ask("运费怎么算", "admin"))
                .isInstanceOf(ApiException.class)
                .satisfies(err -> assertThat(((ApiException) err).code()).isEqualTo("AI_CHAT_FAILED"));
    }
}