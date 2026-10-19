package com.fullmall.support.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.catalog.app.CatalogService;
import com.fullmall.catalog.domain.Product;
import com.fullmall.catalog.domain.Sku;
import com.fullmall.platform.error.ApiException;
import com.fullmall.support.api.SupportDtos.AgentView;
import com.fullmall.support.api.SupportDtos.ClaimRequest;
import com.fullmall.support.api.SupportDtos.ConversationView;
import com.fullmall.support.api.SupportDtos.MessageView;
import com.fullmall.support.api.SupportDtos.SendHelpLinkRequest;
import com.fullmall.support.api.SupportDtos.SendMessageRequest;
import com.fullmall.support.api.SupportDtos.SendProductRequest;
import com.fullmall.support.api.SupportDtos.TransferRequest;
import com.fullmall.support.infra.SupportRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SupportService {

    private final SupportRepository repository;
    private final CatalogService catalogService;
    private final ObjectMapper objectMapper;

    public SupportService(SupportRepository repository, CatalogService catalogService,
                          ObjectMapper objectMapper) {
        this.repository = repository;
        this.catalogService = catalogService;
        this.objectMapper = objectMapper;
    }

    public ConversationView create(UUID orderId, String locale) {
        return create(orderId, locale, "在线咨询", null, null);
    }

    public ConversationView create(UUID orderId, String locale, String subject,
                                   String customerName, String customerPhone) {
        String effectiveLocale = locale == null || locale.isBlank() ? "zh-CN" : locale;
        String effectiveSubject = subject == null || subject.isBlank() ? "在线咨询" : subject.trim();
        return repository.create(orderId, effectiveLocale, effectiveSubject, customerName, customerPhone);
    }

    public List<ConversationView> listConversations(UUID agentId, String status) {
        return repository.list(agentId, status);
    }

    public List<AgentView> agents() {
        return repository.agents();
    }

    @Transactional
    public ConversationView claim(UUID conversationId, ClaimRequest request) {
        ConversationView conversation = requireConversation(conversationId);
        if (conversation.agentId() != null && !conversation.agentId().equals(request.agentId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SUPPORT_CONVERSATION_ASSIGNED",
                    "会话已分配给其他客服");
        }
        if (conversation.agentId() == null && !repository.claim(conversationId, request.agentId())) {
            throw ApiException.conflict("SUPPORT_CLAIM_RACE", "会话已被其他客服领取");
        }
        return requireConversation(conversationId);
    }

    @Transactional
    public ConversationView transfer(UUID conversationId, TransferRequest request) {
        ConversationView conversation = requireConversation(conversationId);
        if (conversation.agentId() != null && !conversation.agentId().equals(request.fromAgentId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SUPPORT_TRANSFER_FORBIDDEN",
                    "只能转交自己负责的会话");
        }
        if (!repository.transfer(conversationId, request.fromAgentId(), request.toAgentId())) {
            throw ApiException.conflict("SUPPORT_TRANSFER_FAILED", "会话归属已变化，请刷新后重试");
        }
        repository.insertTransfer(conversationId, request.fromAgentId(), request.toAgentId(), request.reason());
        return requireConversation(conversationId);
    }

    public List<MessageView> messages(UUID conversationId, UUID agentId) {
        ConversationView conversation = requireConversation(conversationId);
        if (agentId != null && conversation.agentId() != null
                && !conversation.agentId().equals(agentId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SUPPORT_CONVERSATION_FORBIDDEN",
                    "只能查看分配给自己的会话");
        }
        return repository.messages(conversationId);
    }

    @Transactional
    public MessageView sendMessage(UUID conversationId, SendMessageRequest request) {
        requireAssigned(conversationId, request.agentId());
        return repository.insertMessage(conversationId, "AGENT", request.agentId(),
                "TEXT", request.content().trim(), null);
    }

    @Transactional
    public MessageView sendProduct(UUID conversationId, SendProductRequest request) {
        requireAssigned(conversationId, request.agentId());
        Product product = catalogService.getProduct(request.productId(), "zh-CN");
        Sku sku = product.skus().stream().findFirst()
                .orElseThrow(() -> ApiException.conflict("SUPPORT_PRODUCT_NO_SKU", "商品没有可售规格"));
        String mode = request.mode() == null || request.mode().isBlank() ? "PERMANENT" : request.mode();
        String link = "/product/" + product.id() + "?ref=" + request.agentId() + "&mode=" + mode;
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("productId", product.id());
        metadata.put("title", product.title());
        metadata.put("skuCode", sku.skuCode());
        metadata.put("priceAmount", sku.price().amount());
        metadata.put("currency", sku.price().currency());
        metadata.put("link", link);
        metadata.put("referralMode", mode);
        metadata.put("referrerId", request.agentId());
        return repository.insertMessage(conversationId, "AGENT", request.agentId(),
                "PRODUCT", "推荐商品：" + product.title(), writeJson(metadata));
    }

    @Transactional
    public MessageView sendHelpLink(UUID conversationId, SendHelpLinkRequest request) {
        requireAssigned(conversationId, request.agentId());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("title", request.title().trim());
        metadata.put("url", request.url().trim());
        return repository.insertMessage(conversationId, "AGENT", request.agentId(),
                "HELP_LINK", request.title().trim(), writeJson(metadata));
    }

    private ConversationView requireConversation(UUID conversationId) {
        ConversationView conversation = repository.find(conversationId);
        if (conversation == null) {
            throw ApiException.notFound("SUPPORT_CONVERSATION_NOT_FOUND", "会话不存在：" + conversationId);
        }
        return conversation;
    }

    private void requireAssigned(UUID conversationId, UUID agentId) {
        ConversationView conversation = requireConversation(conversationId);
        if (conversation.agentId() == null) {
            throw ApiException.conflict("SUPPORT_CONVERSATION_UNASSIGNED", "请先领取该会话");
        }
        if (!conversation.agentId().equals(agentId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SUPPORT_CONVERSATION_FORBIDDEN",
                    "只能操作分配给自己的会话");
        }
    }

    private String writeJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize support metadata", exception);
        }
    }
}