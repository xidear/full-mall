package com.fullmall.order.app;

import com.fullmall.inventory.domain.LocationAvailability;
import com.fullmall.inventory.domain.LocationType;
import com.fullmall.order.domain.FulfillmentMode;
import com.fullmall.platform.error.ApiException;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class FulfillmentPlanner {

    private FulfillmentPlanner() {
    }

    public static LocationAvailability choose(List<LocationAvailability> locations, int quantity,
                                              FulfillmentMode mode, UUID pickupStoreId) {
        if (mode == FulfillmentMode.PICKUP) {
            if (pickupStoreId == null) {
                throw ApiException.badRequest("STORE_REQUIRED", "storeId is required for pickup");
            }
            return locations.stream()
                    .filter(location -> location.locationType() == LocationType.STORE)
                    .filter(location -> location.locationId().equals(pickupStoreId))
                    .filter(location -> location.availableQuantity() >= quantity)
                    .findFirst()
                    .orElseThrow(() -> ApiException.conflict("PICKUP_STOCK_UNAVAILABLE",
                            "Pickup store " + pickupStoreId + " does not have enough stock"));
        }

        return locations.stream()
                .filter(location -> location.availableQuantity() >= quantity)
                .sorted(Comparator
                        .comparingInt((LocationAvailability location) -> priority(location.locationType()))
                        .thenComparing(LocationAvailability::availableQuantity, Comparator.reverseOrder()))
                .findFirst()
                .orElseThrow(() -> ApiException.conflict("STOCK_UNAVAILABLE",
                        "No location has enough stock for requested quantity " + quantity));
    }

    private static int priority(LocationType type) {
        return switch (type) {
            case WAREHOUSE -> 0;
            case STORE -> 1;
            case SUPPLIER -> 2;
        };
    }
}