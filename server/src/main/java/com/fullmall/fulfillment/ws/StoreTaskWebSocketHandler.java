package com.fullmall.fulfillment.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.fulfillment.domain.StoreTaskChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StoreTaskWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(StoreTaskWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final Map<UUID, Set<WebSocketSession>> sessionsByStore = new ConcurrentHashMap<>();

    public StoreTaskWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID storeId = storeId(session);
        if (storeId == null) {
            session.close(CloseStatus.BAD_DATA.withReason("storeId is required"));
            return;
        }
        sessionsByStore.computeIfAbsent(storeId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        send(session, Map.of("type", "connected", "storeId", storeId.toString()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID storeId = storeId(session);
        if (storeId == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionsByStore.get(storeId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionsByStore.remove(storeId);
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskChanged(StoreTaskChangedEvent event) {
        Set<WebSocketSession> sessions = sessionsByStore.get(event.storeId());
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        Map<String, Object> payload = Map.of(
                "type", "store_task_changed",
                "taskId", event.taskId().toString(),
                "changeType", event.changeType());
        for (WebSocketSession session : sessions) {
            try {
                send(session, payload);
            } catch (IOException exception) {
                log.warn("Failed to push store task event to session {}; closing it", session.getId(), exception);
                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (IOException closeException) {
                    log.warn("Failed to close store websocket session {}", session.getId(), closeException);
                }
                sessions.remove(session);
            }
        }
    }

    private void send(WebSocketSession session, Map<String, Object> payload) throws IOException {
        String json = objectMapper.writeValueAsString(payload);
        synchronized (session) {
            session.sendMessage(new TextMessage(json));
        }
    }

    private UUID storeId(WebSocketSession session) {
        if (session.getUri() == null) {
            return null;
        }
        String raw = UriComponentsBuilder.fromUri(session.getUri()).build()
                .getQueryParams().getFirst("storeId");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}