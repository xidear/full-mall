package com.fullmall.support.api;

import com.fullmall.support.api.SupportDtos.ConversationView;
import com.fullmall.support.app.SupportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/support/conversations")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationView create(@Valid @RequestBody CreateConversationRequest request) {
        return supportService.create(request.orderId(), request.locale(),
                request.subject(), request.customerName(), request.customerPhone());
    }

    public record CreateConversationRequest(
            @NotBlank String subject,
            UUID orderId,
            String locale,
            String customerName,
            String customerPhone
    ) {
    }
}
