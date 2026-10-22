package com.vex.queryer.example.demo;

import com.vex.queryer.JpaQueriesExecutor;
import com.vex.queryer.QueryFieldValidationException;
import com.vex.queryer.QueryValueValidationException;
import com.vex.queryer.QueriesExecutor;
import com.vex.queryer.example.custom.SensitiveFieldGuard;
import com.vex.queryer.example.domain.ArticleEntity;
import com.vex.queryer.example.domain.ArticleRepository;
import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesOperatorEnum;
import com.vex.queryer.model.QueriesPageRequest;
import jakarta.persistence.EntityManager;

/**
 * 角度3 — 自定义检验器(写 SQL 前拦截业务禁止的查询)。
 *
 * <p>展示:注册业务检验器后,敏感字段被拒(字段维度异常)、正则操作符被拒(值维度异常),
 * 正常字段不受影响;异常可分别映射 400。
 */
public class GuardDemo {

    public static void run(ArticleRepository repository, EntityManager entityManager) {
        System.out.println("\n========== 角度3: 自定义检验器(写 SQL 前拦截) ==========");
        DemoData.seed(repository);

        QueriesExecutor<ArticleEntity> executor = JpaQueriesExecutor.of(ArticleEntity.class, entityManager)
                .registerGuard(new SensitiveFieldGuard());   // 业务规则:content 敏感、exp 不开放

        // 敏感字段被拒 → 字段维度异常
        QueriesPageRequest badField = new QueriesPageRequest();
        badField.addAndCondition(QueriesCondition.of("content", QueriesOperatorEnum.eq, "x"));
        try {
            executor.page(badField);
        } catch (QueryFieldValidationException ex) {
            System.out.println("content 被拦截 → QueryFieldValidationException: " + ex.getMessage());
        }

        // 正则操作符被拒 → 值维度异常(业务规则拒绝该操作符)
        QueriesPageRequest badOp = new QueriesPageRequest();
        badOp.addAndCondition(QueriesCondition.of("title", QueriesOperatorEnum.exp, ".*"));
        try {
            executor.page(badOp);
        } catch (QueryValueValidationException ex) {
            System.out.println("exp 被拦截 → QueryValueValidationException: " + ex.getMessage());
        }

        // 正常字段不受影响
        QueriesPageRequest ok = new QueriesPageRequest();
        ok.addAndCondition(QueriesCondition.of("title", QueriesOperatorEnum.eq, "数据域控制"));
        System.out.println("title eq 正常放行: " + executor.page(ok).size() + " 行");
    }
}
