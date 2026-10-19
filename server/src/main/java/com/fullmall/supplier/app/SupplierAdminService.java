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

    public SupplierAdminService(SupplierRepository repository, SupplierSecretCipher cipher) {
        this.repository = repository;
        this.cipher = cipher;
    }

    public List<SupplierCredentialView> listSuppliers() {
        return repository.listSuppliers();
    }

    @Transactional
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