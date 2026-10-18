package com.fullmall.catalog.domain;

import java.util.UUID;

public record Category(UUID id, UUID parentId, String code, String name) {
}