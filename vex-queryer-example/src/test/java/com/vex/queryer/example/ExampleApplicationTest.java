package com.vex.queryer.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 上下文冒烟测试 — 启动即执行 {@link DemoRunner} 全部示例;
 * 任一示例抛出未捕获异常即测试失败。
 */
@SpringBootTest
class ExampleApplicationTest {

    @Test
    void contextLoads() {
        // DemoRunner 在上下文启动阶段已跑完全部示例
    }
}
