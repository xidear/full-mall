package com.fullmall.inventory.domain;

import java.util.UUID;

public record LocationAvailability(LocationType locationType, UUID locationId, int availableQuantity) {
}