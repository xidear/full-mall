package com.fullmall.test;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 集成测试用的真实 PostgreSQL 提供器。
 *
 * <p>优先使用 Testcontainers（与生产同版本 postgres:17）；宿主机没有 Docker 时，
 * **显式告警**并退回嵌入式 PostgreSQL（io.zonky.test，真实 PostgreSQL 进程，非 H2 模拟）。
 * 两条路径都是真实数据库，差别只在版本与启动方式，且日志会写明用了哪条路径，
 * 不会把“没有数据库”伪装成通过。
 */
public final class PostgresTestDatabase {

    private static final Logger log = LoggerFactory.getLogger(PostgresTestDatabase.class);

    private static PostgresTestDatabase instance;

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String mode;

    private PostgresTestDatabase(String jdbcUrl, String username, String password, String mode) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.mode = mode;
    }

    public static synchronized PostgresTestDatabase get() {
        if (instance == null) {
            instance = start();
            Runtime.getRuntime().addShutdownHook(new Thread(PostgresTestDatabase::stopQuietly));
        }
        return instance;
    }

    private static PostgresTestDatabase start() {
        Throwable dockerFailure = null;
        try {
            if (DockerClientFactory.instance().isDockerAvailable()) {
                PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:17-alpine");
                container.start();
                log.info("集成测试使用 Testcontainers postgres:17-alpine (jdbcUrl={})", container.getJdbcUrl());
                CONTAINER = container;
                return new PostgresTestDatabase(container.getJdbcUrl(), container.getUsername(),
                        container.getPassword(), "testcontainers");
            }
            dockerFailure = new IllegalStateException("Docker 客户端不可用");
        } catch (RuntimeException | LinkageError error) {
            dockerFailure = error;
        }

        try {
            EmbeddedPostgres postgres = EmbeddedPostgres.builder().start();
            EMBEDDED = postgres;
            String url = postgres.getJdbcUrl("postgres", "postgres");
            log.warn("Docker 不可用（{}）→ 集成测试改用嵌入式 PostgreSQL {}", 
                    dockerFailure == null ? "未知原因" : dockerFailure.getMessage(), url);
            return new PostgresTestDatabase(url, "postgres", "", "embedded-postgres");
        } catch (Exception error) {
            throw new IllegalStateException(
                    "既无法使用 Docker，也无法启动嵌入式 PostgreSQL；集成测试无法进行。"
                            + "Docker 失败原因: " + (dockerFailure == null ? "未知" : dockerFailure.getMessage()),
                    error);
        }
    }

    private static volatile PostgreSQLContainer<?> CONTAINER;
    private static volatile EmbeddedPostgres EMBEDDED;

    private static void stopQuietly() {
        try {
            if (CONTAINER != null) {
                CONTAINER.stop();
            }
            if (EMBEDDED != null) {
                EMBEDDED.close();
            }
        } catch (Exception error) {
            log.warn("关闭测试数据库时出错: {}", error.getMessage());
        }
    }

    public String jdbcUrl() {
        return jdbcUrl;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    /**
     * @return {@code testcontainers} 或 {@code embedded-postgres}，用于在报告里区分实际运行方式。
     */
    public String mode() {
        return mode;
    }
}