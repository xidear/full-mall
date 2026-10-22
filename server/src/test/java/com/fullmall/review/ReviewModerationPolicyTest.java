package com.fullmall.review;

import com.fullmall.review.app.ReviewModerationPolicy;
import com.fullmall.review.moderation.ContentModeration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;

/** 审核策略：敏感词拒绝、无供应商转人工、供应商异常转人工、通过则发布。 */
class ReviewModerationPolicyTest {

    private ReviewModerationPolicy policy(ContentModeration provider, boolean auto) {
        var factory = new StaticListableBeanFactory();
        if (provider != null) {
            factory.addBean("moderation", provider);
        }
        return new ReviewModerationPolicy(factory.getBeanProvider(ContentModeration.class), auto, "违禁词");
    }

    @Test
    void blockedWordsAreRejectedBeforeCallingProvider() {
        ContentModeration provider = new ContentModeration() {
            public String provider() { return "fake"; }
            public Verdict review(String text) { throw new AssertionError("不应调用供应商"); }
        };
        var result = policy(provider, true).decide("这里有违禁词");

        assertThat(result.outcome()).isEqualTo(ReviewModerationPolicy.Outcome.REJECT);
        assertThat(result.reason()).contains("敏感词");
    }

    @Test
    void missingProviderFallsBackToManual() {
        var result = policy(null, true).decide("东西不错，物流快");

        assertThat(result.outcome()).isEqualTo(ReviewModerationPolicy.Outcome.PENDING);
        assertThat(result.reason()).contains("未配置审核供应商");
    }

    @Test
    void providerExceptionBecomesManualNotApprove() {
        ContentModeration broken = new ContentModeration() {
            public String provider() { return "broken"; }
            public Verdict review(String text) { throw new IllegalStateException("超时"); }
        };
        var result = policy(broken, true).decide("正常评价内容");

        assertThat(result.outcome()).isEqualTo(ReviewModerationPolicy.Outcome.PENDING);
        assertThat(result.reason()).contains("异常");
    }

    @Test
    void providerApprovePublishes() {
        ContentModeration ok = new ContentModeration() {
            public String provider() { return "fake"; }
            public Verdict review(String text) { return Verdict.approve(); }
        };
        assertThat(policy(ok, true).decide("好评").outcome())
                .isEqualTo(ReviewModerationPolicy.Outcome.PUBLISH);
    }
}
