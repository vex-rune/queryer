package com.vex.queryer.example.demo;

import com.vex.queryer.JpaQueriesExecutor;
import com.vex.queryer.example.domain.ArticleEntity;
import com.vex.queryer.example.domain.ArticleRepository;
import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesOperatorEnum;
import com.vex.queryer.model.QueriesOrder;
import com.vex.queryer.model.QueriesPage;
import com.vex.queryer.model.QueriesPageRequest;
import jakarta.persistence.EntityManager;

import java.util.List;

/**
 * 角度1 — 基础三件套查询(predicate / order / page / count / exists)。
 *
 * <p>展示:枚举按 {@code @JsonValue} 外部码归一、时间字符串自动归一为 {@code Instant}
 * 再与 timestamptz 列比较、分页从 0 起始、count 复用同一谓词。
 */
public class BasicQueryDemo {

    public static void run(ArticleRepository repository, EntityManager entityManager) {
        System.out.println("\n========== 角度1: 基础三件套查询(predicate/order/page) ==========");
        DemoData.seed(repository);

        QueriesPageRequest req = new QueriesPageRequest();
        // 状态按 @JsonValue 外部码 "1"(=PUBLISHED);内置枚举转换器负责归一
        req.addAndCondition(QueriesCondition.of("status", QueriesOperatorEnum.eq, "1"));
        // 时间条件直接用 ISO 字符串;内置时间转换器归一为 Instant 再与列比较
        req.addAndCondition(QueriesCondition.of("publishedAt", QueriesOperatorEnum.gte, "2026-10-01T00:00:00Z"));
        req.setOrder(List.of(new QueriesOrder("viewCount", QueriesOrder.QueriesOrderEnum.DESC)));
        req.setPage(new QueriesPage(0, 2));

        JpaQueriesExecutor<ArticleEntity> executor = JpaQueriesExecutor.of(ArticleEntity.class, entityManager);

        List<ArticleEntity> page = executor.page(req);
        long total = executor.count();                    // 复用同一 predicate
        boolean exists = executor.exists(req.getPredicate());

        System.out.println("PUBLISHED 且 2026-10-01 之后,按 viewCount 倒序 page(0,2): "
                + page.stream().map(ArticleEntity::getTitle).toList());
        System.out.println("count(复用谓词) = " + total);
        System.out.println("exists(命中) = " + exists);

        // 模糊匹配
        QueriesPageRequest like = new QueriesPageRequest();
        like.addAndCondition(QueriesCondition.of("title", QueriesOperatorEnum.like, "%实战%"));
        System.out.println("title like '%实战%': " + executor.page(like).stream().map(ArticleEntity::getTitle).toList());
    }
}
