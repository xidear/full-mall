package com.fullmall.notification;

import com.fullmall.notification.channel.WechatTemplateSender;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 微信模板消息：缺配置启动失败；access_token 只取一次（缓存）；errcode≠0 视为失败。 */
class WechatTemplateSenderTest {

    @Test
    void missingConfigFailsFast() {
        assertThatThrownBy(() -> new WechatTemplateSender("", "secret", "tpl", null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("需配置");
        assertThatThrownBy(() -> new WechatTemplateSender("app", "secret", "", null))
                .hasMessageContaining("需配置");
    }

    @Test
    void sendsTemplateAndCachesToken() throws Exception {
        AtomicInteger tokenCalls = new AtomicInteger();
        AtomicInteger sendCalls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/cgi-bin/token", exchange -> {
            tokenCalls.incrementAndGet();
            byte[] body = "{\"access_token\":\"TOKEN-1\",\"expires_in\":7200}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/cgi-bin/message/template/send", exchange -> {
            sendCalls.incrementAndGet();
            String query = exchange.getRequestURI().getQuery();
            if (query == null || !query.contains("TOKEN-1")) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }
            byte[] body = "{\"errcode\":0,\"errmsg\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            var sender = new WechatTemplateSender("app", "secret", "tpl", base);
            var notification = new com.fullmall.notification.domain.Notification(
                    java.util.UUID.randomUUID(), "CUSTOMER", "openid-1", "ORDER", "WECHAT",
                    "标题", "内容", "PENDING", null, null, null, java.time.Instant.now());

            sender.send(notification);
            sender.send(notification);

            assertThat(tokenCalls.get()).as("token 必须缓存").isEqualTo(1);
            assertThat(sendCalls.get()).isEqualTo(2);
        } finally {
            server.stop(0);
        }
    }
}
