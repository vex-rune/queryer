package com.vex.queryer.example.demo;

import com.vex.queryer.QueriesExecutor;
import com.vex.queryer.example.custom.InMemoryQueriesExecutor;
import com.vex.queryer.example.custom.SkuCodeValueConverter;
import com.vex.queryer.example.custom.SubjectScopeFilter;
import com.vex.queryer.example.domain.ArticleEntity;
import com.vex.queryer.example.domain.ArticleRepository;
import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesCriteria;
import com.vex.queryer.model.QueriesOperatorEnum;
import com.vex.queryer.model.QueriesPage;
import com.vex.queryer.model.QueriesPageRequest;
import com.vex.queryer.model.QueriesPredicate;

import java.util.List;

/**
 * 角度7 — 自定义存储执行器(面向 {@link QueriesExecutor} 抽象编程)。
 *
 * <p>用内存执行器(不连 DB)演示:存储实现只管"把谓词翻译成存储语义",
 * 检验器 / 转换器 / 数据域 / 限制器全部由抽象基类保证一致。
 * 未来实现 Spring Data MongoDB 的 {@code DataQueriesExecutor} 即按此模板。
 */
public class CustomExecutorDemo {

    public static void run(ArticleRepository repository) {
        System.out.println("\n========== 角度7: 自定义存储执行器(面向 QueriesExecutor 抽象) ==========");
        DemoData.seed(repository);

        List<ArticleEntity> all = repository.findAll();

        // 链式装配:自定义转换器 + 数据域过滤;全部在内存执行
        QueriesExecutor<ArticleEntity> executor = new InMemoryQueriesExecutor<>(all)
                .registerConverter(new SkuCodeValueConverter())
                .dataScope(new SubjectScopeFilter(() -> DemoData.SUBJECT_A));

        QueriesPageRequest req = new QueriesPageRequest();
        req.addAndCondition(QueriesCondition.of("skuCode", QueriesOperatorEnum.eq, "sku-a"));
        req.setPage(new QueriesPage(0, 10));

        List<ArticleEntity> page = executor.page(req);
        long total = executor.count();   // 复用同一谓词 + 数据域
        System.out.println("内存执行: subject=" + DemoData.SUBJECT_A + " 且 skuCode='sku-a' → "
                + page.stream().map(ArticleEntity::getTitle).toList());
        System.out.println("内存 count(复用谓词) = " + total);

        QueriesPredicate published = new QueriesPredicate();
        published.setAnd(List.of(QueriesCriteria.of(
                QueriesCondition.of("status", QueriesOperatorEnum.eq, "PUBLISHED"))));
        System.out.println("内存 exists(PUBLISHED, 数据域A) = " + executor.exists(published));

        // 不带数据域与谓词的纯全量
        QueriesExecutor<ArticleEntity> plain = new InMemoryQueriesExecutor<>(all);
        System.out.println("无数据域 list() = " + plain.list().size() + " 行");
    }
}
