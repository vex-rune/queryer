package com.vex.queryer.example.demo;

import com.vex.queryer.JpaQueriesExecutor;
import com.vex.queryer.QueriesExecutor;
import com.vex.queryer.example.custom.SkuCodeValueConverter;
import com.vex.queryer.example.domain.ArticleEntity;
import com.vex.queryer.example.domain.ArticleRepository;
import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesOperatorEnum;
import com.vex.queryer.model.QueriesPageRequest;
import jakarta.persistence.EntityManager;

import java.util.List;

/**
 * 角度2 — 值转换器(内置 + 自定义注册)。
 *
 * <p>展示:内置转换器(枚举按 name / @JsonValue、时间、数值、布尔)免费可用;
 * 业务自定义类型通过 {@code registerConverter(...)} 追加,查询值在写 SQL 前归一。
 */
public class ConverterDemo {

    public static void run(ArticleRepository repository, EntityManager entityManager) {
        System.out.println("\n========== 角度2: 值转换器(内置 + 自定义) ==========");
        DemoData.seed(repository);

        // 链式注册返回基类 QueriesExecutor;查询入口一致
        QueriesExecutor<ArticleEntity> executor = JpaQueriesExecutor.of(ArticleEntity.class, entityManager)
                .registerConverter(new SkuCodeValueConverter());   // 追加自定义转换器

        // 内置枚举转换器:按 name 与 @JsonValue 外部码均可
        QueriesPageRequest byName = new QueriesPageRequest();
        byName.addAndCondition(QueriesCondition.of("status", QueriesOperatorEnum.eq, "PUBLISHED"));
        System.out.println("status eq 'PUBLISHED'(name): " + executor.page(byName).size() + " 行");

        // 自定义转换器:skuCode 输入小写,归一为大写后匹配存储值
        QueriesPageRequest bySku = new QueriesPageRequest();
        bySku.addAndCondition(QueriesCondition.of("skuCode", QueriesOperatorEnum.eq, "sku-a"));
        List<ArticleEntity> rows = executor.page(bySku);
        System.out.println("skuCode eq 'sku-a'(自定义转换器归一): " + rows.stream().map(ArticleEntity::getTitle).toList());

        // in 集合元素同样逐项归一
        QueriesPageRequest in = new QueriesPageRequest();
        in.addAndCondition(QueriesCondition.of("skuCode", QueriesOperatorEnum.in, List.of("sku-a", "sku-e")));
        System.out.println("skuCode in ['sku-a','sku-e']: " + executor.page(in).size() + " 行");
    }
}
