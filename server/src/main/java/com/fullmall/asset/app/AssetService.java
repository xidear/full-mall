package com.fullmall.asset.app;

import com.fullmall.asset.domain.AssetEntity;
import com.fullmall.asset.infra.AssetRepository;
import com.fullmall.platform.crud.BaseScopedService;
import com.fullmall.platform.scope.Scope;
import com.fullmall.platform.audit.AuditTrail;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 素材库服务。每个写操作都提交 JaVers 审计（谁、何时、哪些字段、旧值/新值）。
 */
@Service
public class AssetService extends BaseScopedService<AssetEntity, AssetRepository> {

    private final AuditTrail auditTrail;

    public AssetService(AssetRepository repository, AuditTrail auditTrail) {
        super(repository);
        this.auditTrail = auditTrail;
    }

    @Override
    protected String ownerOf(AssetEntity entity) {
        return entity.getOwnerUsername();
    }

    @Override
    protected Page<AssetEntity> findOwned(String ownerKey, Pageable pageable) {
        return repository.findByOwnerUsernameAndDeletedAtIsNull(ownerKey, pageable);
    }

    @Transactional
    public AssetEntity createAsset(Scope scope, AssetEntity entity, UUID actorId, String actorName) {
        AssetEntity saved = create(scope, entity, actorId);
        auditTrail.commit(actorName, saved);
        return saved;
    }

    @Transactional
    public AssetEntity updateAsset(Scope scope, UUID id, UUID actorId, String actorName) {
        AssetEntity saved = update(scope, id, actorId);
        auditTrail.commit(actorName, saved);
        return saved;
    }

    @Transactional
    public void deleteAsset(Scope scope, UUID id, UUID actorId, String actorName) {
        AssetEntity entity = getScoped(scope, id);
        softDelete(scope, id, actorId);
        auditTrail.commit(actorName, entity);
    }

    public List<CdoSnapshot> history(UUID id) {
        return auditTrail.history(id, AssetEntity.class);
    }
}