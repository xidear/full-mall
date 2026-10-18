package com.fullmall.store.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.store.domain.NearbyStore;
import com.fullmall.store.domain.Store;
import com.fullmall.store.infra.StoreRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class StoreService {

    private static final double EARTH_RADIUS_METERS = 6_371_000d;

    private final StoreRepository repository;

    public StoreService(StoreRepository repository) {
        this.repository = repository;
    }

    public List<NearbyStore> findNearby(UUID skuId, BigDecimal latitude, BigDecimal longitude, int limit) {
        List<Store> stores = skuId == null
                ? repository.findPickupStores()
                : repository.findPickupStoresWithStock(skuId, LocalDate.now());
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return stores.stream()
                .map(store -> toNearby(store, latitude, longitude))
                .sorted(Comparator
                        .comparing(NearbyStore::distanceMeters, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(NearbyStore::name))
                .limit(safeLimit)
                .toList();
    }

    public Store getStore(UUID storeId) {
        Store store = repository.findById(storeId);
        if (store == null) {
            throw ApiException.notFound("STORE_NOT_FOUND", "Store not found: " + storeId);
        }
        return store;
    }

    private NearbyStore toNearby(Store store, BigDecimal latitude, BigDecimal longitude) {
        Integer distance = null;
        if (latitude != null && longitude != null && store.latitude() != null && store.longitude() != null) {
            distance = (int) Math.round(distanceMeters(
                    latitude.doubleValue(), longitude.doubleValue(),
                    store.latitude().doubleValue(), store.longitude().doubleValue()));
        }
        return new NearbyStore(
                store.id(), store.name(), store.address(), store.latitude(), store.longitude(),
                store.pickupEnabled(), store.phone(), store.openingHours(), distance);
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}