package com.vex.queryer.example.custom;

import com.vex.queryer.QueryFieldValidationException;
import com.vex.queryer.QueryValueValidationException;
import com.vex.queryer.guard.QueryGuard;
import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesOperatorEnum;

/**
 * 自定义查询检验器 — 在写 SQL 前拦截业务禁止的查询。
 *
 * <p>两条规则:
 * <ul>
 *     <li>{@code content} 视为敏感字段,禁止作为查询条件 → 字段维度异常</li>
 *     <li>{@code exp} 正则操作符本业务不开放 → 值维度异常</li>
 * </ul>
 * 异常继承 {@link QueryValidationException},可直接按字段/值维度映射 400。
 */
public class SensitiveFieldGuard implements QueryGuard {

    @Override
    public void check(QueriesCondition condition, FieldContext context) {
        if ("content".equals(condition.getField())) {
            throw new QueryFieldValidationException("字段受保护,禁止作为查询条件: field=" + condition.getField());
        }
        if (condition.getOp() == QueriesOperatorEnum.exp) {
            throw new QueryValueValidationException("本业务不开放正则操作符: op=" + condition.getOp());
        }
    }
}
