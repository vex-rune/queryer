package com.vex.queryer.model;

/**
 * 查询操作符枚举 — 比较/字符串/集合/空值四类;前端契约值 = 枚举名。
 */
public enum QueriesOperatorEnum {
    /**
     * 等于
     */
    eq,
    /**
     * 不等于
     */
    ne,
    /**
     * 大于(Comparable)
     */
    gt,
    /**
     * 大于等于(Comparable)
     */
    gte,
    /**
     * 小于(Comparable)
     */
    lt,
    /**
     * 小于等于(Comparable)
     */
    lte,
    /**
     * 模糊匹配(等价 SQL LIKE)
     */
    like,
    /**
     * 正则匹配(PostgreSQL 15+ regexp_like)
     */
    exp,
    /**
     * IN(值必须为 Collection)
     */
    in,
    /**
     * IS NULL(值必须传 null)
     */
    isNull,
    /**
     * IS NOT NULL(值必须传 null)
     */
    isNotNull
}
