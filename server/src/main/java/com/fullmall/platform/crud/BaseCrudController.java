package com.fullmall.platform.crud;

import com.fullmall.platform.domain.BaseEntity;
import com.fullmall.platform.scope.Scope;
import com.fullmall.platform.scope.ScopeResolver;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * CRUD 基类（见 TECHNICAL_ARCHITECTURE_V2 §3）。
 *
 * <p>子类只实现 scope/DTO 映射与专有动作，不得在 Controller 里拼 SQL、返回实体或自行实现分页/权限。
 */
public abstract class BaseCrudController<E extends BaseEntity, DTO, CREATE, UPDATE> {

    private final ScopeResolver scopeResolver;

    protected BaseCrudController(ScopeResolver scopeResolver) {
        this.scopeResolver = scopeResolver;
    }

    protected abstract BaseScopedService<E, ?> service();

    protected abstract DTO toDto(E entity);

    protected abstract E fromCreate(CREATE request, Scope scope, Authentication authentication);

    protected abstract void applyUpdate(E entity, UPDATE request, Scope scope);

    /**
     * 审计操作人 UUID：来自 {@link ActorDetails}；未提供时返回 null（宁可留空也不伪造）。
     */
    protected UUID actorId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object details = authentication.getDetails();
        if (details instanceof com.fullmall.platform.scope.ActorDetails actor) {
            return actor.id();
        }
        return null;
    }

    protected Scope scope(Authentication authentication) {
        return scopeResolver.resolve(authentication);
    }

    @GetMapping
    public Page<DTO> list(Pageable pageable, Authentication authentication) {
        return service().listScoped(scope(authentication), pageable).map(this::toDto);
    }

    @GetMapping("/{id}")
    public DTO get(@PathVariable UUID id, Authentication authentication) {
        return toDto(service().getScoped(scope(authentication), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DTO create(@Valid @RequestBody CREATE request, Authentication authentication) {
        Scope scope = scope(authentication);
        E entity = fromCreate(request, scope, authentication);
        return toDto(service().create(scope, entity, actorId(authentication)));
    }

    @PatchMapping("/{id}")
    public DTO update(@PathVariable UUID id, @Valid @RequestBody UPDATE request, Authentication authentication) {
        Scope scope = scope(authentication);
        E entity = service().getScoped(scope, id);
        applyUpdate(entity, request, scope);
        return toDto(service().update(scope, id, actorId(authentication)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        service().softDelete(scope(authentication), id, actorId(authentication));
    }
}