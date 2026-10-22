package com.vex.queryer.example;

import com.vex.queryer.example.demo.BasicQueryDemo;
import com.vex.queryer.example.demo.ConverterDemo;
import com.vex.queryer.example.demo.CustomExecutorDemo;
import com.vex.queryer.example.demo.DataScopeDemo;
import com.vex.queryer.example.demo.ExceptionDemo;
import com.vex.queryer.example.demo.GuardDemo;
import com.vex.queryer.example.demo.LimitsDemo;
import com.vex.queryer.example.domain.ArticleRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 示例编排 — 依次执行全部角度;任一示例抛异常则直接失败(便于作为冒烟测试)。
 */
@Component
public class DemoRunner implements CommandLineRunner {

    private final ArticleRepository repository;
    private final EntityManager entityManager;

    public DemoRunner(ArticleRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n\n========== vex-queryer 使用示例(共 7 个角度) ==========");
        BasicQueryDemo.run(repository, entityManager);
        ConverterDemo.run(repository, entityManager);
        GuardDemo.run(repository, entityManager);
        DataScopeDemo.run(repository, entityManager);
        LimitsDemo.run(repository, entityManager);
        ExceptionDemo.run(repository, entityManager);
        CustomExecutorDemo.run(repository);
        System.out.println("\n========== 全部示例执行完毕 ==========");
    }
}
