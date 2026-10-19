package com.fullmall.support.api;

import com.fullmall.support.api.SupportDtos.AgentView;
import com.fullmall.support.api.SupportDtos.ClaimRequest;
import com.fullmall.support.api.SupportDtos.ConversationView;
import com.fullmall.support.api.SupportDtos.MessageView;
import com.fullmall.support.api.SupportDtos.SendHelpLinkRequest;
import com.fullmall.support.api.SupportDtos.SendMessageRequest;
import com.fullmall.support.api.SupportDtos.SendProductRequest;
import com.fullmall.support.api.SupportDtos.TransferRequest;
import com.fullmall.support.app.SupportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/support/v1")
public class SupportWorkbenchController {

    private final SupportService supportService;

    public SupportWorkbenchController(SupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping("/agents")
    public List<AgentView> agents() {
        return supportService.agents();
    }

    @GetMapping("/conversations")
    public List<ConversationView> conversations(@RequestParam(required = false) UUID agentId,
                                                @RequestParam(required = false) String status) {
        return supportService.listConversations(agentId, status);
    }

    @PostMapping("/conversations/{conversationId}/claim")
    public ConversationView claim(@PathVariable UUID conversationId,
                                  @Valid @RequestBody ClaimRequest request) {
        return supportService.claim(conversationId, request);
    }

    @PostMapping("/conversations/{conversationId}/transfer")
    public ConversationView transfer(@PathVariable UUID conversationId,
                                     @Valid @RequestBody TransferRequest request) {
        return supportService.transfer(conversationId, request);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<MessageView> messages(@PathVariable UUID conversationId,
                                      @RequestParam(required = false) UUID agentId) {
        return supportService.messages(conversationId, agentId);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public MessageView sendMessage(@PathVariable UUID conversationId,
                                   @Valid @RequestBody SendMessageRequest request) {
        return supportService.sendMessage(conversationId, request);
    }

    @PostMapping("/conversations/{conversationId}/product")
    public MessageView sendProduct(@PathVariable UUID conversationId,
                                   @Valid @RequestBody SendProductRequest request) {
        return supportService.sendProduct(conversationId, request);
    }

    @PostMapping("/conversations/{conversationId}/help-link")
    public MessageView sendHelpLink(@PathVariable UUID conversationId,
                                    @Valid @RequestBody SendHelpLinkRequest request) {
        return supportService.sendHelpLink(conversationId, request);
    }
}
