package com.fullmall.asset;

import com.fullmall.asset.app.AssetService;
import com.fullmall.asset.domain.AssetEntity;
import com.fullmall.asset.infra.AssetRepository;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.scope.Scope;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 P0 公共基座：行级范围、软删除、乐观锁、JaVers 审计，
 * 并用真实 PostgreSQL 验证 {@code ddl-auto=validate} 的实体与 Flyway schema 一致。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class AssetScopeAndAuditTest {

    private static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
        } catch (Exception error) {
            throw new IllegalStateException("无法启动嵌入式 PostgreSQL，测试无法进行", error);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    AssetService service;

    @Autowired
    AssetRepository repository;

    @Autowired
    com.fullmall.asset.api.AssetController assetController;

    private static final UUID ADMIN_ACTOR = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private AssetEntity createFor(String owner) {
        AssetEntity asset = new AssetEntity("素材-" + owner, "https://cdn.example.com/" + owner + ".png", "IMAGE", owner);
        return service.createAsset(Scope.owner(owner), asset, ADMIN_ACTOR, owner);
    }

    @Test
    void controllerIsDeniedWhenMethodSecurityHasNoAuthentication() {
        // 方法级授权必须生效：没有认证时不能靠“前端隐藏按钮”绕过
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        assetController.list(PageRequest.of(0, 10), null))
                // 无认证 → AuthenticationException；有认证但角色不足 → AccessDeniedException
                .isInstanceOfAny(org.springframework.security.core.AuthenticationException.class,
                        org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void ownerCannotReadAnotherOwnersAsset() {
        AssetEntity alice = createFor("alice");
        AssetEntity bob = createFor("bob");

        assertThat(service.getScoped(Scope.owner("alice"), alice.getId()).getId()).isEqualTo(alice.getId());

        // 越权按 404 处理，不泄露资源是否存在
        assertThatThrownBy(() -> service.getScoped(Scope.owner("alice"), bob.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("资源不存在");
    }

    @Test
    void listIsScopedAndAdminSeesAll() {
        createFor("carol");
        createFor("dave");

        List<String> carolOwners = service.listScoped(Scope.owner("carol"), PageRequest.of(0, 50))
                .map(AssetEntity::getOwnerUsername).getContent();
        assertThat(carolOwners).containsOnly("carol");

        List<String> adminOwners = service.listScoped(Scope.all(), PageRequest.of(0, 100))
                .map(AssetEntity::getOwnerUsername).getContent();
        assertThat(adminOwners).contains("carol", "dave");
    }

    @Test
    void softDeletedAssetDisappearsForOwner() {
        AssetEntity asset = createFor("erin");
        service.deleteAsset(Scope.owner("erin"), asset.getId(), ADMIN_ACTOR, "erin");

        assertThatThrownBy(() -> service.getScoped(Scope.owner("erin"), asset.getId()))
                .isInstanceOf(ApiException.class);
        assertThat(service.listScoped(Scope.owner("erin"), PageRequest.of(0, 50)).getContent())
                .noneMatch(row -> row.getId().equals(asset.getId()));
    }

    @Test
    void staleUpdateIsRejectedByOptimisticLock() {
        AssetEntity asset = createFor("frank");

        AssetEntity first = repository.findById(asset.getId()).orElseThrow();
        AssetEntity stale = repository.findById(asset.getId()).orElseThrow();

        first.setName("素材-frank-v2");
        repository.saveAndFlush(first);

        stale.setName("素材-frank-v3");
        assertThatThrownBy(() -> repository.saveAndFlush(stale))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void everyWriteIsAuditedByJavers() {
        AssetEntity asset = createFor("gina");
        List<CdoSnapshot> afterCreate = service.history(asset.getId());
        assertThat(afterCreate).isNotEmpty();
        assertThat(afterCreate.get(0).getCommitMetadata().getAuthor()).isEqualTo("gina");

        AssetEntity loaded = service.getScoped(Scope.owner("gina"), asset.getId());
        loaded.setName("素材-gina-改名");
        service.updateAsset(Scope.owner("gina"), asset.getId(), ADMIN_ACTOR, "gina");

        List<CdoSnapshot> afterUpdate = service.history(asset.getId());
        assertThat(afterUpdate.size()).isGreaterThan(afterCreate.size());
        assertThat(afterUpdate).anySatisfy(snapshot ->
                assertThat(snapshot.getChanged()).contains("name"));
    }
}