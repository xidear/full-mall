package com.fullmall.logistics;

import com.fullmall.logistics.app.Kuaidi100Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 快递100 报文解析：正常、失败、结构变化都要有确定行为。 */
class Kuaidi100ResponseTest {

    @Test
    void parsesSuccessResponse() {
        String body = """
                {"message":"ok","state":"0","status":"200","data":[
                  {"time":"2026-10-20 10:00:00","context":"已揽收","areaName":"上海市"},
                  {"time":"2026-10-20 18:30:00","context":"运输中","areaName":"杭州转运中心"}
                ]}
                """;

        var events = Kuaidi100Response.parse(body);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).status()).isEqualTo("已揽收");
        assertThat(events.get(0).location()).isEqualTo("上海市");
        assertThat(events.get(1).at()).isAfter(events.get(0).at());
    }

    @Test
    void failureStatusThrowsInsteadOfEmptyList() {
        String body = "{\"status\":\"201\",\"message\":\"查无结果\"}";

        assertThatThrownBy(() -> Kuaidi100Response.parse(body))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("查无结果");
    }

    @Test
    void malformedOrMissingFieldsThrow() {
        assertThatThrownBy(() -> Kuaidi100Response.parse("not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不是合法 JSON");

        assertThatThrownBy(() -> Kuaidi100Response.parse("{\"status\":\"200\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("缺少 data");

        assertThatThrownBy(() -> Kuaidi100Response.parse(
                "{\"status\":\"200\",\"data\":[{\"time\":\"\"}]}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("缺少 time/context");
    }
}
