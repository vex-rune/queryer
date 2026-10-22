package com.vex.queryer.example.demo;

import com.vex.queryer.JpaQueriesExecutor;
import com.vex.queryer.QueryLimits;
import com.vex.queryer.QueryValidationException;
import com.vex.queryer.example.domain.ArticleEntity;
import com.vex.queryer.example.domain.ArticleRepository;
import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesCriteria;
import com.vex.queryer.model.QueriesOperatorEnum;
import com.vex.queryer.model.QueriesOrder;
import com.vex.queryer.model.QueriesPageRequest;
import com.vex.queryer.model.QueriesPredicate;
import jakarta.persistence.EntityManager;

import java.util.List;

/**
 * 角度5 — 限制器(默认嵌套 3 层;构造可覆盖)。
 *
 * <p>展示:默认限制拒绝 4 层嵌套;构造传入 {@link QueryLimits} 放宽深度;
 * 排序字段数上限收紧后同样被拒。
 */
public class LimitsDemo {

    public static void run(ArticleRepository repository, EntityManager entityManager) {
        System.out.println("\n========== 角度5: 限制器(默认 3 层嵌套,构造可覆盖) ==========");
        DemoData.seed(repository);

        // 默认限制:4 层嵌套被拒
        QueriesPageRequest deep = new QueriesPageRequest();
        deep.setPredicate(nest(4));
        try {
            JpaQueriesExecutor.of(ArticleEntity.class, entityManager).page(deep);
        } catch (QueryValidationException ex) {
            System.out.println("默认限制拒 4 层嵌套: " + ex.getMessage());
        }

        // 构造放宽到 5 层 → 放行
        List<ArticleEntity> rows = JpaQueriesExecutor.of(ArticleEntity.class, entityManager,
                new QueryLimits(200, 10, 5)).page(deep);
        System.out.println("自定义 QueryLimits(200,10,5) 放行 4 层嵌套 → " + rows.size() + " 行");

        // 排序数上限收紧为 1,2 个排序字段被拒
        QueriesPageRequest twoOrders = new QueriesPageRequest();
        twoOrders.setOrder(List.of(
                new QueriesOrder("viewCount", QueriesOrder.QueriesOrderEnum.DESC),
                new QueriesOrder("title", QueriesOrder.QueriesOrderEnum.ASC)));
        try {
            JpaQueriesExecutor.of(ArticleEntity.class, entityManager,
                    new QueryLimits(200, 1, 3)).page(twoOrders);
        } catch (QueryValidationException ex) {
            System.out.println("排序数超限被拒: " + ex.getMessage());
        }
    }

    /// 构造恰好 levels 层嵌套的谓词(根计第 1 层);最深层挂叶子 title = 数据域控制
    private static QueriesPredicate nest(int levels) {
        QueriesPredicate root = new QueriesPredicate();
        QueriesPredicate cursor = root;
        for (int i = 1; i < levels; i++) {
            QueriesPredicate inner = new QueriesPredicate();
            QueriesCriteria wrapper = new QueriesCriteria();
            wrapper.setPredicate(inner);
            cursor.setAnd(List.of(wrapper));
            cursor = inner;
        }
        cursor.setAnd(List.of(QueriesCriteria.of(
                QueriesCondition.of("title", QueriesOperatorEnum.eq, "数据域控制"))));
        return root;
    }
}
