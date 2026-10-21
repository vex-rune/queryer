package com.vex.queryer.guard;

import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesOrder;

import java.util.regex.Pattern;

/**
 * 字段与规模检验器 — 拦截字段名穿透与条件/排序数量爆炸。
 *
 * <p>字段名虽然已由 {@code root.get(field)} 校验存在性,但仍可能携带路径语义
 * ({@code owner.secret})触发关联穿透;条件数量无上限时会生成巨型 WHERE 子句,
 * 让 DB 生成器超时或计划缓存失效。
 */
public class FieldAndScaleGuard implements QueryGuard {

    /// 单次查询允许的最大条件数
    public static final int MAX_CONDITIONS = 200;

    /// 单次查询允许的最大排序数
    private static final int MAX_ORDERS = 10;

    /// 字段名合法形态 — 纯标识符,禁止路径分隔与空格
    private static final Pattern FIELD_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]{0,63}$");

    @Override
    public void check(QueriesCondition condition, FieldContext context) {
        checkFieldName(condition.getField());
    }

    @Override
    public void checkOrder(QueriesOrder order, FieldContext context) {
        checkFieldName(order.getField());
    }

    /**
     * 校验字段名 — 纯标识符,拒绝路径穿透与非法字符
     *
     * @param field 字段名
     */
    public static void checkFieldName(String field) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("查询字段不能为空");
        }
        if (!FIELD_PATTERN.matcher(field).matches()) {
            throw new IllegalArgumentException("不合法的查询字段名: field=" + field
                    + ", 期望=字母或下划线开头的标识符(禁止路径分隔符)");
        }
    }

    /**
     * 校验排序数量上限
     *
     * @param size 排序项数量
     */
    public static void checkOrderCount(int size) {
        if (size > MAX_ORDERS) {
            throw new IllegalArgumentException("排序字段数量超限: count=" + size + ", 上限=" + MAX_ORDERS);
        }
    }
}