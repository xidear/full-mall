package com.fullmall.platform.crud;

import com.fullmall.platform.domain.BaseEntity;
import com.fullmall.platform.error.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * 带行级范围与软删除的服务基类（见 TECHNICAL_ARCHITECTURE_V2 §2.5）。
 *
 * <p>越权读取统一抛 404，不暴露资源是否存在；软删除后同样视为不存在。
 */
public abstract class BaseScopedService<E extends BaseEntity, R extends JpaRepository<E, UUID>> {

    protected final R repository;

    protected BaseScopedService(R repository) {
        this.repository = repository;
    }

    /** 数据行的归属人标识，用于与 {@link com.fullmall.platform.scope.Scope} 比对。 */
    protected abstract String ownerOf(E entity);

    /** 按归属人分页查询；实现方用仓储查询方法完成，禁止 Controller 手写 SQL。 */
    protected abstract Page<E> findOwned(String ownerKey, Pageable pageable);

    public E getScoped(com.fullmall.platform.scope.Scope scope, UUID id) {
        E entity = repository.findById(id)
                .orElseThrow(() -> notFound(id));
        if (entity.isDeleted() || !scope.allows(ownerOf(entity))) {
            throw notFound(id);
        }
        return entity;
    }

    public Page<E> listScoped(com.fullmall.platform.scope.Scope scope, Pageable pageable) {
        if (scope.isAll()) {
            return repository.findAll(pageable);
        }
        return findOwned(scope.ownerKey(), pageable);
    }

    @Transactional
    public E create(com.fullmall.platform.scope.Scope scope, E entity, UUID actorId) {
        Instant now = Instant.now();
        entity.setId(UUID.randomUUID());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(actorId);
        entity.setUpdatedBy(actorId);
        return repository.save(entity);
    }

    @Transactional
    public E update(com.fullmall.platform.scope.Scope scope, UUID id, UUID actorId) {
        E entity = getScoped(scope, id);
        entity.setUpdatedAt(Instant.now());
        entity.setUpdatedBy(actorId);
        return repository.save(entity);
    }

    @Transactional
    public void softDelete(com.fullmall.platform.scope.Scope scope, UUID id, UUID actorId) {
        E entity = getScoped(scope, id);
        entity.setDeletedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        entity.setUpdatedBy(actorId);
        repository.save(entity);
    }

    private ApiException notFound(UUID id) {
        return ApiException.notFound("NOT_FOUND", "资源不存在: " + id);
    }
}