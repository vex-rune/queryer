package com.vex.queryer.example.custom;

import com.vex.queryer.DataScopeFilter;
import com.vex.queryer.example.domain.ArticleEntity;
import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesOperatorEnum;

import java.util.List;
import java.util.function.Supplier;

/**
 * 数据域过滤器 — 按"当前主体 id"自动追加 {@code authorId = ?} 过滤。
 *
 * <p>主体 id 由 {@code Supplier} 提供,来源完全自定义:固定死值、ThreadLocal 缓存、查库等,
 * "怎么取"由调用方决定;取不到(返回 null/空白)则本次不过滤。
 *
 * <p>注册后,执行器的 page / list / count / exists 全部查询都会以 AND 语义自动带上数据域条件。
 */
public class SubjectScopeFilter implements DataScopeFilter<ArticleEntity> {

    private final Supplier<String> subjectIdSupplier;

    public SubjectScopeFilter(Supplier<String> subjectIdSupplier) {
        this.subjectIdSupplier = subjectIdSupplier;
    }

    @Override
    public List<QueriesCondition> conditions() {
        String subjectId = subjectIdSupplier.get();
        if (subjectId == null || subjectId.isBlank()) {
            return null; // 无上下文 → 本次不过滤
        }
        return List.of(QueriesCondition.of("authorId", QueriesOperatorEnum.eq, subjectId));
    }
}
