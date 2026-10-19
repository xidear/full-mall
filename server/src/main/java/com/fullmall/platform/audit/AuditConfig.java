package com.fullmall.platform.audit;

import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.repository.sql.DialectName;
import org.javers.repository.sql.JaversSqlRepository;
import org.javers.repository.sql.SqlRepositoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 审计装配（JaVers 7.11.9，SQL 仓储）。
 *
 * <p>为什么不用 starter：`javers-spring-boot-starter-sql` 7.11.9 面向 Spring Boot 4
 * （依赖 `org.springframework.boot.jpa.autoconfigure.JpaProperties`），本项目是 Boot 3.4。
 * 因此手工装配，并让 **Flyway 管理 jv_* 表**（V24），库自身的建表能力默认关闭。
 */
@Configuration
public class AuditConfig {

    /**
     * JaVers 7.11.9 的 SQL 仓储不会关闭连接（实测：5 次 commit 打开 37 个连接、关闭 0 个），
     * 直接接连接池会在 20 次提交后耗尽池子。
     *
     * <p>因此这里**固定复用一条连接**，并把所有审计调用串行化（见 {@link AuditTrail}）：
     * 连接数恒为 1，不会泄漏；代价是审计写入串行。升级 JaVers 或改回连接池前必须重跑
     * {@code AssetScopeAndAuditTest} 覆盖该约束。
     */
    @Bean
    public JaversSqlRepository javersSqlRepository(
            DataSource dataSource,
            @Value("${app.audit.schema-management:false}") boolean schemaManagement) {
        DialectName dialect = detectDialect(dataSource);
        SingleConnectionProvider provider = new SingleConnectionProvider(dataSource);
        return SqlRepositoryBuilder.sqlRepository()
                .withDialect(dialect)
                .withConnectionProvider(provider)
                .withSchemaManagementEnabled(schemaManagement)
                .build();
    }

    @Bean
    public Javers javers(JaversSqlRepository repository) {
        return JaversBuilder.javers()
                .registerJaversRepository(repository)
                .build();
    }

    /**
     * 复用单条连接（JaVers 不归还连接，见 {@link #javersSqlRepository} 的说明）。
     */
    static final class SingleConnectionProvider implements org.javers.repository.sql.ConnectionProvider {

        private final DataSource dataSource;
        private Connection connection;

        SingleConnectionProvider(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public synchronized Connection getConnection() throws SQLException {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                if (!connection.getAutoCommit()) {
                    connection.setAutoCommit(true);
                }
            }
            return connection;
        }
    }

    private DialectName detectDialect(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product != null && product.toLowerCase().contains("postgres")) {
                return DialectName.POSTGRES;
            }
            if (product != null && product.toLowerCase().contains("h2")) {
                return DialectName.H2;
            }
            throw new IllegalStateException(
                    "无法确定 JaVers 方言，数据库产品名为: " + product + "（支持 PostgreSQL/H2）");
        } catch (SQLException error) {
            throw new IllegalStateException("连接数据库以确定 JaVers 方言失败", error);
        }
    }
}