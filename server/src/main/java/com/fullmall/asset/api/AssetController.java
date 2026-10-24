package com.fullmall.asset.api;

import com.fullmall.asset.app.AssetService;
import com.fullmall.asset.domain.AssetEntity;
import com.fullmall.platform.crud.BaseCrudController;
import com.fullmall.platform.crud.BaseScopedService;
import com.fullmall.platform.scope.Scope;
import com.fullmall.platform.scope.ScopeResolver;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * 素材库后台接口。除专有动作外，列表/详情/新建/修改/删除全部来自 {@link BaseCrudController}。
 */
@RestController
@RequestMapping("/admin/v1/assets")
public class AssetController extends BaseCrudController<AssetEntity, AssetController.AssetDto,
        AssetController.CreateAssetRequest, AssetController.UpdateAssetRequest> {

    private final AssetService assetService;

    public AssetController(ScopeResolver scopeResolver, AssetService assetService) {
        super(scopeResolver);
        this.assetService = assetService;
    }

    public record AssetDto(UUID id, long version, String name, String url, String kind,
                           String ownerUsername, String purpose, Instant createdAt, Instant updatedAt) {
    }

    public record CreateAssetRequest(@NotBlank @Size(max = 200) String name,
                                     @NotBlank @Size(max = 1024) String url,
                                     @NotBlank @Size(max = 32) String kind,
                                     String purpose) {
    }

    public record UpdateAssetRequest(@Size(max = 200) String name,
                                     @Size(max = 1024) String url,
                                     @Size(max = 32) String kind) {
    }

    @Override
    protected BaseScopedService<AssetEntity, ?> service() {
        return assetService;
    }

    @Override
    protected AssetDto toDto(AssetEntity entity) {
        return new AssetDto(entity.getId(), entity.getVersion(), entity.getName(), entity.getUrl(),
                entity.getKind(), entity.getOwnerUsername(), entity.getPurpose(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    @Override
    protected AssetEntity fromCreate(CreateAssetRequest request, Scope scope, Authentication authentication) {
        if (scope.isAll()) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "OWNER_REQUIRED", "平台管理员创建素材时需要通过 /admin/v1/assets/for/{owner} 指定归属人");
        }
        AssetEntity entity = new AssetEntity(request.name().trim(), request.url().trim(),
                request.kind().trim(), scope.ownerKey());
        entity.setPurpose(request.purpose() == null || request.purpose().isBlank()
                ? "PRODUCT" : request.purpose().trim().toUpperCase(java.util.Locale.ROOT));
        return entity;
    }

    @Override
    protected void applyUpdate(AssetEntity entity, UpdateAssetRequest request, Scope scope) {
        if (request.name() != null) {
            entity.setName(request.name().trim());
        }
        if (request.url() != null) {
            entity.setUrl(request.url().trim());
        }
        if (request.kind() != null) {
            entity.setKind(request.kind().trim());
        }
    }

    @Override
    protected UUID actorId(Authentication authentication) {
        return super.actorId(authentication);
    }

    // 方法级授权必须写在具体映射方法上：类级 @PreAuthorize 不会作用于基类继承的 MVC 处理方法（已实测）。

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping
    public Page<AssetDto> list(Pageable pageable, Authentication authentication) {
        return super.list(pageable, authentication);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping("/{id}")
    public AssetDto get(@PathVariable UUID id, Authentication authentication) {
        return super.get(id, authentication);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssetDto create(@Valid @RequestBody CreateAssetRequest request, Authentication authentication) {
        return super.create(request, authentication);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @PatchMapping("/{id}")
    public AssetDto update(@PathVariable UUID id, @Valid @RequestBody UpdateAssetRequest request,
                           Authentication authentication) {
        return super.update(id, request, authentication);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        super.delete(id, authentication);
    }
}