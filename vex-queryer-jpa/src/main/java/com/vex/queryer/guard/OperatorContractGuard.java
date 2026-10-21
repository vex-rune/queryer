package com.vex.queryer.guard;

import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesOperatorEnum;

import java.util.Collection;

/**
 * 操作符契约检验器 — 校验值与操作符的匹配关系。
 *
 * <p>这一类是执行器原有 {@code requireXxx} 守卫的等价物,收敛到可插拔形态:
 * 值类型对不上时在写 SQL 前拦下,而不是让 DB 抛
 * {@code operator does not exist: ... varchar} 这类难以定位的错误。
 */
public class OperatorContractGuard implements QueryGuard {

    @Override
    public void check(QueriesCondition condition, FieldContext context) {
        QueriesOperatorEnum op = condition.getOp();
        Object value = condition.getValue();
        switch (op) {
            case gt, gte, lt, lte -> requireComparable(value, op, condition.getField());
            case like -> requireString(value, op, condition.getField());
            case exp -> {
                requireString(value, op, condition.getField());
                requireSafeRegex(value, condition.getField());
            }
            case in -> requireCollection(value, op, condition.getField());
            case isNull, isNotNull -> requireNull(value, op, condition.getField());
            case eq, ne -> {
                // 等值/不等值允许任意类型,不额外约束
            }
        }
    }

    private void requireComparable(Object value, QueriesOperatorEnum op, String field) {
        if (!(value instanceof Comparable)) {
            throw new IllegalArgumentException("操作符与值类型不匹配: field=" + field
                    + ", op=" + op
                    + ", value=" + describe(value)
                    + ", 期望=Comparable 类型");
        }
    }

    private void requireString(Object value, QueriesOperatorEnum op, String field) {
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("操作符与值类型不匹配: field=" + field
                    + ", op=" + op
                    + ", value=" + describe(value)
                    + ", 期望=String");
        }
    }

    private void requireCollection(Object value, QueriesOperatorEnum op, String field) {
        if (!(value instanceof Collection<?> collection)) {
            throw new IllegalArgumentException("操作符与值类型不匹配: field=" + field
                    + ", op=" + op
                    + ", value=" + describe(value)
                    + ", 期望=集合");
        }
        if (collection.isEmpty()) {
            throw new IllegalArgumentException("in 操作符集合不能为空: field=" + field);
        }
    }

    private void requireNull(Object value, QueriesOperatorEnum op, String field) {
        if (value != null) {
            throw new IllegalArgumentException("操作符不接受值: field=" + field
                    + ", op=" + op
                    + ", value=" + describe(value));
        }
    }

    /// 正则类输入必须不含 NUL;过长的模式串在 PG 上会拖垮执行器
    private void requireSafeRegex(Object value, String field) {
        String pattern = (String) value;
        if (pattern.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("exp 正则含非法字符: field=" + field + ", 期望=不含 NUL");
        }
        if (pattern.length() > 1000) {
            throw new IllegalArgumentException("exp 正则过长: field=" + field
                    + ", length=" + pattern.length() + ", 上限=1000");
        }
    }

    private String describe(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }
}