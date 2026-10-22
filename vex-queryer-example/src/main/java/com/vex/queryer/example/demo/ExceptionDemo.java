package com.vex.queryer.example.demo;

import com.vex.queryer.JpaQueriesExecutor;
import com.vex.queryer.QueryFieldValidationException;
import com.vex.queryer.QueryValueValidationException;
import com.vex.queryer.example.domain.ArticleEntity;
import com.vex.queryer.example.domain.ArticleRepository;
import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesOperatorEnum;
import com.vex.queryer.model.QueriesPageRequest;
import jakarta.persistence.EntityManager;

import java.util.List;

/**
 * 角度6 — 异常细分(字段异常 vs 值异常,便于统一映射 400)。
 *
 * <p>展示:非法字段抛 {@link QueryFieldValidationException};值格式/类型不匹配抛
 * {@link QueryValueValidationException};业务可分别捕获给出不同错误码。
 */
public class ExceptionDemo {

    public static void run(ArticleRepository repository, EntityManager entityManager) {
        System.out.println("\n========== 角度6: 异常细分(字段异常 vs 值异常) ==========");
        DemoData.seed(repository);
        JpaQueriesExecutor<ArticleEntity> executor = JpaQueriesExecutor.of(ArticleEntity.class, entityManager);

        // 字段维度:字段不存在
        QueriesPageRequest noField = new QueriesPageRequest();
        noField.addAndCondition(QueriesCondition.of("noSuchField", QueriesOperatorEnum.eq, "x"));
        try {
            executor.page(noField);
        } catch (QueryFieldValidationException ex) {
            System.out.println("字段异常 → 可映射 400(field 错误): " + ex.getMessage());
        }

        // 值维度:时间格式非法
        QueriesPageRequest badValue = new QueriesPageRequest();
        badValue.addAndCondition(QueriesCondition.of("publishedAt", QueriesOperatorEnum.gte, "不是时间"));
        try {
            executor.page(badValue);
        } catch (QueryValueValidationException ex) {
            System.out.println("值异常 → 可映射 400(value 错误): " + ex.getMessage());
        }

        // 值维度:in 空集合
        QueriesPageRequest emptyIn = new QueriesPageRequest();
        emptyIn.addAndCondition(QueriesCondition.of("status", QueriesOperatorEnum.in, List.of()));
        try {
            executor.page(emptyIn);
        } catch (QueryValueValidationException ex) {
            System.out.println("值异常(in 空集合): " + ex.getMessage());
        }
    }
}
