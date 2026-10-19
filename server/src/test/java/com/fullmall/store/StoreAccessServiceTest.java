package com.fullmall.store;

import com.fullmall.platform.error.ApiException;
import com.fullmall.staff.app.StaffAuthService;
import com.fullmall.staff.domain.StaffUser;
import com.fullmall.store.app.StoreAccessService;
import com.fullmall.store.app.StoreService;
import com.fullmall.store.domain.Store;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StoreAccessServiceTest {

    private static final UUID SHANGHAI = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BEIJING = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final StaffUser STORE_USER = new StaffUser(UUID.randomUUID(), "store", "门店", "STORE", "ACTIVE");
    private static final StaffUser ADMIN = new StaffUser(UUID.randomUUID(), "admin", "管理员", "ADMIN", "ACTIVE");

    private final StaffAuthService staffAuthService = mock(StaffAuthService.class);
    private final StoreService storeService = mock(StoreService.class);

    @Test
    void storeUserCanOnlyAccessBoundStore() {
        StoreAccessService service = new StoreAccessService(staffAuthService, storeService, true);
        when(staffAuthService.storeIds(STORE_USER.id())).thenReturn(List.of(SHANGHAI));

        service.requireStore(STORE_USER, SHANGHAI);
        assertThatThrownBy(() -> service.requireStore(STORE_USER, BEIJING))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("未绑定");
    }

    @Test
    void adminBypassesStoreScope() {
        StoreAccessService service = new StoreAccessService(staffAuthService, storeService, true);
        service.requireStore(ADMIN, BEIJING);
    }

    @Test
    void accessibleStoresFiltersByBinding() {
        StoreAccessService service = new StoreAccessService(staffAuthService, storeService, true);
        when(staffAuthService.storeIds(STORE_USER.id())).thenReturn(List.of(SHANGHAI));
        when(storeService.listAll()).thenReturn(List.of(store(SHANGHAI, "上海静安店"), store(BEIJING, "北京朝阳店")));

        List<Store> stores = service.accessibleStores(STORE_USER);

        assertThat(stores).extracting(Store::id).containsExactly(SHANGHAI);
    }

    @Test
    void unboundStoreUserFailsClosed() {
        StoreAccessService service = new StoreAccessService(staffAuthService, storeService, true);
        when(staffAuthService.storeIds(STORE_USER.id())).thenReturn(List.of());
        when(storeService.listAll()).thenReturn(List.of(store(SHANGHAI, "上海静安店")));

        assertThatThrownBy(() -> service.accessibleStores(STORE_USER))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("尚未绑定");
    }

    @Test
    void prodRequiresLoginWhenStoreAuthEnabled() {
        StoreAccessService service = new StoreAccessService(staffAuthService, storeService, true);
        assertThatThrownBy(() -> service.accessibleStores(null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("请先登录");
    }

    private Store store(UUID id, String name) {
        return new Store(id, name, "地址", BigDecimal.ZERO, BigDecimal.ZERO, true, null, null);
    }
}