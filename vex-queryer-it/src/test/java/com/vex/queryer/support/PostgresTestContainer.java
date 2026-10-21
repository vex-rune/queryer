package com.vex.queryer.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * PostgreSQL 测试容器基类 — 全容器共享一个实例,启动后建好表。
 *
 * <p>必须用真实 PostgreSQL 而非 H2:{@code exp} 操作符依赖 PG 15+ 的
 * {@code regexp_like},且原始故障({@code timestamptz >= character varying})
 * 本身就是 PG 方言行为,H2 复现不出来。
 */
@Testcontainers
public abstract class PostgresTestContainer {

    /// PG 15+ 才有 regexp_like;与执行器 exp 策略的方言要求一致
    @Container
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("queryer_it")
                    .withUsername("queryer")
                    .withPassword("queryer");

    /**
     * 容器启动后建表 — 单例容器只建一次。
     *
     * @throws Exception 连接或建表失败
     */
    @org.junit.jupiter.api.BeforeAll
    static void initSchema() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS it_device (
                        id            VARCHAR(64) PRIMARY KEY,
                        code          VARCHAR(64)  NOT NULL,
                        name          VARCHAR(128),
                        device_status VARCHAR(32),
                        device_role   VARCHAR(32),
                        weight        INTEGER,
                        enabled       BOOLEAN,
                        created_at    TIMESTAMPTZ,
                        remark        VARCHAR(256)
                    )
                    """);
        }
    }

    /**
     * 动态注入数据源配置 — 容器 JDBC URL 在运行期才确定。
     *
     * @param registry 属性注册器
     */
    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}