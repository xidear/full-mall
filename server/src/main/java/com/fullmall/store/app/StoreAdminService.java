package com.fullmall.store.app;

import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.validation.ValidationRules;
import com.fullmall.store.domain.Store;
import com.fullmall.store.infra.StoreRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 门店后台管理（列表 / 新建 / 编辑 / 启停）。
 *
 * <p>为什么状态用 active 布尔而不是删除：门店被订单、库存批次、POS 班次引用，
 * 删除会让历史数据失去落点；停用是唯一安全动作，所以只提供启停。
 *
 * <p>编码唯一性由 {@code store_code_key} 约束兜底，服务层先查一次只为把
 * SQL 异常翻译成运营看得懂的错误码；并发插入仍可能撞键，因此同时捕获 DuplicateKeyException。
 */
@Service
public class StoreAdminService {

    private static final String CODE_PATTERN = "^[A-Z0-9][A-Z0-9_-]{1,63}$";
    private static final int NAME_MAX = 128;
    private static final int ADDRESS_MAX = 255;
    private static final int OPENING_HOURS_MAX = 128;

    private final StoreRepository repository;
    private final AuditEventService auditEvents;

    public StoreAdminService(StoreRepository repository, AuditEventService auditEvents) {
        this.repository = repository;
        this.auditEvents = auditEvents;
    }

    public record Command(String code, String marketCode, String name, String address,
                          BigDecimal latitude, BigDecimal longitude, boolean pickupEnabled,
                          String phone, String openingHours) {
    }

    public List<Store> list(boolean includeInactive) {
        return repository.findAll(includeInactive);
    }

    @Transactional
    public Store create(Command command, String actor) {
        requireActor(actor);
        Command normalized = normalize(command);
        validate(normalized, null);
        Store store = new Store(UUID.randomUUID(), normalized.code(), normalized.marketCode(),
                normalized.name(), normalized.address(), normalized.latitude(), normalized.longitude(),
                normalized.pickupEnabled(), true, normalized.phone(), normalized.openingHours(),
                Instant.now());
        try {
            repository.insert(store);
        } catch (DuplicateKeyException exception) {
            throw ApiException.conflict("STORE_CODE_EXISTS", "门店编码已存在：" + normalized.code());
        }
        audit("STORE_CREATED", store, actor, "code=" + store.code() + " market=" + store.marketCode());
        return store;
    }

    @Transactional
    public Store update(UUID storeId, Command command, String actor) {
        requireActor(actor);
        Store existing = load(storeId);
        Command normalized = normalize(command);
        validate(normalized, storeId);
        Store updated = new Store(existing.id(), normalized.code(), normalized.marketCode(),
                normalized.name(), normalized.address(), normalized.latitude(), normalized.longitude(),
                normalized.pickupEnabled(), existing.active(), normalized.phone(),
                normalized.openingHours(), existing.createdAt());
        int rows = repository.update(updated);
        if (rows != 1) {
            throw ApiException.notFound("STORE_NOT_FOUND", "门店不存在：" + storeId);
        }
        audit("STORE_UPDATED", updated, actor, "code=" + updated.code());
        return updated;
    }

    /**
     * 停用不检查"是否有员工绑定"：解绑是另一件事，且停用本身可逆；
     * 若在这里拒绝，运营会先去删员工，反而丢审计线索。
     */
    @Transactional
    public Store setActive(UUID storeId, boolean active, String actor) {
        requireActor(actor);
        Store existing = load(storeId);
        if (existing.active() == active) {
            return existing;
        }
        int rows = repository.updateActive(storeId, active);
        if (rows != 1) {
            throw ApiException.notFound("STORE_NOT_FOUND", "门店不存在：" + storeId);
        }
        audit("STORE_STATUS_CHANGED", existing, actor,
                "code=" + existing.code() + " active=" + existing.active() + "->" + active);
        return new Store(existing.id(), existing.code(), existing.marketCode(), existing.name(),
                existing.address(), existing.latitude(), existing.longitude(),
                existing.pickupEnabled(), active, existing.phone(), existing.openingHours(),
                existing.createdAt());
    }

    private Store load(UUID storeId) {
        if (storeId == null) {
            throw ApiException.badRequest("STORE_ID_REQUIRED", "必须指定门店");
        }
        Store store = repository.findById(storeId, true);
        if (store == null) {
            throw ApiException.notFound("STORE_NOT_FOUND", "门店不存在：" + storeId);
        }
        return store;
    }

    private Command normalize(Command command) {
        if (command == null) {
            throw ApiException.badRequest("STORE_FIELDS_REQUIRED", "门店信息不能为空");
        }
        String code = trimUpper(command.code());
        String market = trimUpper(command.marketCode());
        String phone = trim(command.phone());
        String openingHours = trim(command.openingHours());
        return new Command(code, market, trim(command.name()), trim(command.address()),
                command.latitude(), command.longitude(), command.pickupEnabled(), phone, openingHours);
    }

    private void validate(Command command, UUID storeId) {
        if (command.code().isEmpty()) {
            throw ApiException.badRequest("STORE_CODE_REQUIRED", "门店编码不能为空");
        }
        if (!command.code().matches(CODE_PATTERN)) {
            throw ApiException.badRequest("STORE_CODE_INVALID",
                    "门店编码只能是大写字母/数字/-/_，长度 2..64，实际=" + command.code());
        }
        if (repository.codeExists(command.code(), storeId)) {
            throw ApiException.conflict("STORE_CODE_EXISTS", "门店编码已存在：" + command.code());
        }
        if (command.marketCode().isEmpty()) {
            throw ApiException.badRequest("STORE_MARKET_REQUIRED", "必须选择门店所属市场");
        }
        if (!repository.marketExists(command.marketCode())) {
            throw ApiException.badRequest("STORE_MARKET_NOT_FOUND",
                    "市场不存在：" + command.marketCode());
        }
        if (command.name().isEmpty()) {
            throw ApiException.badRequest("STORE_NAME_REQUIRED", "门店名称不能为空");
        }
        if (command.name().length() > NAME_MAX) {
            throw ApiException.badRequest("STORE_NAME_TOO_LONG",
                    "门店名称不能超过 " + NAME_MAX + " 个字符");
        }
        if (command.address().isEmpty()) {
            throw ApiException.badRequest("STORE_ADDRESS_REQUIRED", "门店地址不能为空");
        }
        if (command.address().length() > ADDRESS_MAX) {
            throw ApiException.badRequest("STORE_ADDRESS_TOO_LONG",
                    "门店地址不能超过 " + ADDRESS_MAX + " 个字符");
        }
        validateCoordinate(command.latitude(), "纬度", 90);
        validateCoordinate(command.longitude(), "经度", 180);
        if (!command.phone().isEmpty() && !command.phone().matches(ValidationRules.PHONE_PATTERN)) {
            throw ApiException.badRequest("STORE_PHONE_INVALID",
                    "门店电话格式不正确（只允许数字、+、-、空格，6..20 位）");
        }
        if (command.openingHours().length() > OPENING_HOURS_MAX) {
            throw ApiException.badRequest("STORE_OPENING_HOURS_TOO_LONG",
                    "营业时间不能超过 " + OPENING_HOURS_MAX + " 个字符");
        }
    }

    private void validateCoordinate(BigDecimal value, String label, int limit) {
        if (value == null) {
            return;
        }
        if (value.abs().compareTo(BigDecimal.valueOf(limit)) > 0) {
            throw ApiException.badRequest("STORE_COORDINATES_INVALID",
                    label + "必须在 -" + limit + ".." + limit + " 之间，实际=" + value);
        }
    }

    private void audit(String type, Store store, String actor, String detail) {
        auditEvents.record(new AuditEventService.AuditEvent(
                type, actor, "store", store.id().toString(), detail, null));
    }

    private void requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw ApiException.badRequest("ACTOR_REQUIRED", "必须记录操作人");
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimUpper(String value) {
        return trim(value).toUpperCase(Locale.ROOT);
    }
}