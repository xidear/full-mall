package com.fullmall.support.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SupportWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SupportWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final String appMode;

    public SupportWebSocketHandler(ObjectMapper objectMapper, @Value("${app.mode:dev}") String appMode) {
        this.objectMapper = objectMapper;
        this.appMode = appMode;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        send(session, Map.of(
                "type", "welcome",
                "readOnly", "demo".equalsIgnoreCase(appMode),
                "protocol", "fullmall.support.v1"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("demo".equalsIgnoreCase(appMode)) {
            send(session, Map.of(
                    "type", "demo_read_only",
                    "message", "The public demo does not accept customer messages."));
            return;
        }
        send(session, Map.of(
                "type", "echo",
                "received", message.getPayload()));
    }

    private void send(WebSocketSession session, Map<String, Object> payload) throws IOException {
        Map<String, Object> ordered = new LinkedHashMap<>(payload);
        ordered.putIfAbsent("ts", System.currentTimeMillis());
        try {
            String json = objectMapper.writeValueAsString(ordered);
            session.sendMessage(new TextMessage(json));
        } catch (JsonProcessingException exception) {
            log.error("Failed to serialize support websocket payload session={}", session.getId(), exception);
            session.close(CloseStatus.SERVER_ERROR.withReason("serialization failed"));
            throw exception;
        } catch (IOException exception) {
            log.error("Failed to send support websocket message session={}", session.getId(), exception);
            throw exception;
        }
    }
}