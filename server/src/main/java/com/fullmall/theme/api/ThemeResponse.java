package com.fullmall.theme.api;

import com.fasterxml.jackson.databind.JsonNode;

public record ThemeResponse(String channel, String market, JsonNode config) {
}