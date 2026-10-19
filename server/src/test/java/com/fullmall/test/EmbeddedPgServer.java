package com.fullmall.test;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * 本地联调用：启动一个嵌入式 PostgreSQL 并保持运行，打印 JDBC URL。
 *
 * <p>仅用于开发联调（容器内没有 Docker 时替代 compose 的 postgres 服务）。
 */
public final class EmbeddedPgServer {

    private EmbeddedPgServer() {
    }

    public static void main(String[] args) throws Exception {
        EmbeddedPostgres postgres = EmbeddedPostgres.builder().setPort(55432).start();
        String url = postgres.getJdbcUrl("postgres", "postgres");
        System.out.println("PG_URL=" + url);
        System.out.println("PG_READY");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                postgres.close();
            } catch (Exception ignored) {
                // 关闭失败不影响进程退出
            }
        }));
        Thread.currentThread().join();
    }
}
