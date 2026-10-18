package com.fullmall.theme.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.platform.error.ApiException;
import com.fullmall.theme.api.ThemeResponse;
import com.fullmall.theme.domain.ThemeConfigRecord;
import com.fullmall.theme.infra.ThemeConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class ThemeConfigService {

    private static final Set<String> CHANNELS = Set.of("PC", "MINI");

    private final ThemeConfigRepository repository;
    private final ObjectMapper objectMapper;

    public ThemeConfigService(ThemeConfigRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public ThemeResponse get(String channel, String market) {
        String normalizedChannel = normalizeChannel(channel);
        String normalizedMarket = normalizeMarket(market);
        ThemeConfigRecord record = repository.find(normalizedChannel, normalizedMarket);
        return new ThemeResponse(normalizedChannel, normalizedMarket, record == null ? null : parse(record.configJson()));
    }

    @Transactional
    public ThemeResponse save(String channel, String market, JsonNode config) {
        String normalizedChannel = normalizeChannel(channel);
        String normalizedMarket = normalizeMarket(market);
        if (config == null || !config.isObject()) {
            throw ApiException.badRequest("INVALID_THEME_CONFIG", "主题配置必须是对象");
        }
        try {
            ThemeConfigRecord saved = repository.upsert(
                    normalizedChannel,
                    normalizedMarket,
                    objectMapper.writeValueAsString(config));
            return new ThemeResponse(saved.channel(), saved.market(), config);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize theme config", exception);
        }
    }

    private JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse stored theme config", exception);
        }
    }

    private String normalizeChannel(String channel) {
        String normalized = channel == null ? "" : channel.trim().toUpperCase();
        if (!CHANNELS.contains(normalized)) {
            throw ApiException.badRequest("INVALID_THEME_CHANNEL", "主题渠道必须是 PC 或 MINI");
        }
        return normalized;
    }

    private String normalizeMarket(String market) {
        String normalized = market == null ? "CN" : market.trim().toUpperCase();
        if (normalized.length() != 2) {
            throw ApiException.badRequest("INVALID_MARKET", "市场代码必须是两位");
        }
        return normalized;
    }
}