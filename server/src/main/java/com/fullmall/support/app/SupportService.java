package com.fullmall.support.app;

import com.fullmall.support.domain.SupportConversation;
import com.fullmall.support.infra.SupportRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SupportService {

    private final SupportRepository repository;

    public SupportService(SupportRepository repository) {
        this.repository = repository;
    }

    public SupportConversation create(UUID orderId, String locale) {
        String effectiveLocale = locale == null || locale.isBlank() ? "zh-CN" : locale;
        return repository.insert(orderId, effectiveLocale);
    }
}