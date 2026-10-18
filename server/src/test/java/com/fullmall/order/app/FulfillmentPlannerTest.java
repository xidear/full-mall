package com.fullmall.order.app;

import com.fullmall.inventory.domain.LocationAvailability;
import com.fullmall.inventory.domain.LocationType;
import com.fullmall.order.domain.FulfillmentMode;
import com.fullmall.platform.error.ApiException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FulfillmentPlannerTest {

    @Test
    void prefersWarehouseForShipping() {
        UUID warehouseId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        List<LocationAvailability> locations = List.of(
                new LocationAvailability(LocationType.SUPPLIER, supplierId, 100),
                new LocationAvailability(LocationType.STORE, storeId, 10),
                new LocationAvailability(LocationType.WAREHOUSE, warehouseId, 5));

        LocationAvailability chosen = FulfillmentPlanner.choose(locations, 5, FulfillmentMode.SHIP, null);

        assertEquals(LocationType.WAREHOUSE, chosen.locationType());
        assertEquals(warehouseId, chosen.locationId());
    }

    @Test
    void pickupMustUseRequestedStore() {
        UUID storeId = UUID.randomUUID();
        List<LocationAvailability> locations = List.of(
                new LocationAvailability(LocationType.WAREHOUSE, UUID.randomUUID(), 100),
                new LocationAvailability(LocationType.STORE, storeId, 5));

        LocationAvailability chosen = FulfillmentPlanner.choose(locations, 3, FulfillmentMode.PICKUP, storeId);

        assertEquals(LocationType.STORE, chosen.locationType());
        assertEquals(storeId, chosen.locationId());
    }

    @Test
    void pickupFailsWhenStoreLacksStock() {
        UUID storeId = UUID.randomUUID();
        List<LocationAvailability> locations = List.of(
                new LocationAvailability(LocationType.STORE, storeId, 2));

        assertThrows(ApiException.class,
                () -> FulfillmentPlanner.choose(locations, 3, FulfillmentMode.PICKUP, storeId));
    }
}