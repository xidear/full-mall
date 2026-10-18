package com.fullmall.analytics.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.analytics.api.AnalyticsOverview;
import com.fullmall.analytics.infra.AnalyticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final AnalyticsRepository repository;
    private final ObjectMapper objectMapper;

    public AnalyticsService(AnalyticsRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(String eventType, UUID orderId, UUID skuId, Map<String, Object> properties) {
        try {
            repository.insert(eventType, orderId, skuId, objectMapper.writeValueAsString(properties));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize analytics properties for " + eventType, exception);
        }
    }

    public AnalyticsOverview overview() {
        return repository.overview();
    }
}