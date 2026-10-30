package com.fullmall.supplier.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.supplier.api.SupplierCredentialCreated;
import com.fullmall.supplier.api.SupplierCredentialView;
import com.fullmall.supplier.infra.SupplierRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class SupplierAdminService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SupplierRepository repository;
    private final SupplierSecretCipher cipher;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final com.fullmall.platform.audit.AuditEventService auditEvents;

    public SupplierAdminService(SupplierRepository repository, SupplierSecretCipher cipher,
                                org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
                                com.fullmall.platform.audit.AuditEventService auditEvents) {
        this.repository = repository;
        this.cipher = cipher;
        this.jdbcTemplate = jdbcTemplate;
        this.auditEvents = auditEvents;
    }

    public List<SupplierCredentialView> listSuppliers() {
        return repository.listSuppliers();
    }

    /** 后台列表排序（白名单在仓储层）。 */
    public List<SupplierCredentialView> listSuppliers(String sort, String direction) {
        return repository.listSuppliers(sort, direction);
    }

    @Transactional
    /**
     * 启用/停用供应商。停用后该供应商的凭证立即不可用（登录态与 token 校验都查 active）。
     *
     * <p>做成独立端点而不是复用"编辑供应商"：列表里点一下开关就生效，
     * 不需要为了改一个布尔值打开整张表单。
     */
    public void setActive(UUID supplierId, boolean active, String actor) {
        int updated = jdbcTemplate.update("UPDATE supplier SET active = ? WHERE id = ?", active, supplierId);
        if (updated != 1) {
            throw com.fullmall.platform.error.ApiException.notFound("SUPPLIER_NOT_FOUND",
                    "供应商不存在: " + supplierId);
        }
        auditEvents.record(new com.fullmall.platform.audit.AuditEventService.AuditEvent(
                "SUPPLIER_STATUS_CHANGED", actor, "supplier", supplierId.toString(),
                "active=" + active, null));
    }

    public SupplierCredentialCreated rotateCredential(UUID supplierId) {
        if (!repository.supplierExists(supplierId)) {
            throw ApiException.notFound("SUPPLIER_NOT_FOUND", "供应商不存在或已停用：" + supplierId);
        }
        String accessKey = "sup_" + randomToken(18);
        String secret = "sec_" + randomToken(32);
        try {
            repository.upsertCredential(supplierId, accessKey, cipher.encrypt(secret));
        } catch (DuplicateKeyException exception) {
            throw ApiException.conflict("SUPPLIER_ACCESS_KEY_COLLISION",
                    "生成的 Access Key 与现有凭证冲突，请重试");
        }
        return new SupplierCredentialCreated(supplierId, accessKey, secret);
    }

    private String randomToken(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }
}