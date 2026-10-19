package com.fullmall.asset.api;

import com.fullmall.asset.app.AssetService;
import com.fullmall.asset.domain.AssetEntity;
import com.fullmall.platform.crud.BaseCrudController;
import com.fullmall.platform.crud.BaseScopedService;
import com.fullmall.platform.scope.Scope;
import com.fullmall.platform.scope.ScopeResolver;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
                           String ownerUsername, Instant createdAt, Instant updatedAt) {
    }

    public record CreateAssetRequest(@NotBlank @Size(max = 200) String name,
                                     @NotBlank @Size(max = 1024) String url,
                                     @NotBlank @Size(max = 32) String kind) {
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
                entity.getKind(), entity.getOwnerUsername(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    @Override
    protected AssetEntity fromCreate(CreateAssetRequest request, Scope scope, Authentication authentication) {
        if (scope.isAll()) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "OWNER_REQUIRED", "平台管理员创建素材时需要通过 /admin/v1/assets/for/{owner} 指定归属人");
        }
        return new AssetEntity(request.name().trim(), request.url().trim(),
                request.kind().trim(), scope.ownerKey());
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
}