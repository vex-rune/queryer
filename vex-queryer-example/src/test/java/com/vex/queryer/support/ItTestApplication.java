package com.vex.queryer.support;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PG 集成测试专用 Spring 应用 — 仅装配 JPA 自动配置与测试实体/仓储。
 *
 * <p>{@code @SpringBootTest} 需要一个 {@code @SpringBootConfiguration} 作为上下文根,
 * 本类提供该入口,作用域限定在测试源码内,不进入发布产物。
 */
@SpringBootApplication
public class ItTestApplication {
}
