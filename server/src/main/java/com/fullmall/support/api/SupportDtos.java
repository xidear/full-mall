package com.fullmall.support.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class SupportDtos {

    private SupportDtos() {
    }

    public record ConversationView(
            UUID id, String subject, UUID orderId, String status, String locale,
            UUID agentId, String agentName, String customerName, String customerPhone,
            Instant createdAt, Instant assignedAt, Instant lastMessageAt
    ) {
    }

    public record MessageView(
            UUID id, String senderType, UUID senderId, String messageType, String content,
            String metadataJson, Instant createdAt
    ) {
    }

    public record AgentView(UUID id, String displayName, String role) {
    }

    public record ClaimRequest(@NotNull UUID agentId) {
    }

    public record TransferRequest(@NotNull UUID fromAgentId, @NotNull UUID toAgentId, String reason) {
    }

    public record SendMessageRequest(@NotNull UUID agentId, @NotBlank String content) {
    }

    public record SendProductRequest(@NotNull UUID agentId, @NotNull UUID productId, String mode) {
    }

    public record SendHelpLinkRequest(@NotNull UUID agentId, @NotBlank String title, @NotBlank String url) {
    }
}
