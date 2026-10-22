package com.vex.queryer;

import java.io.Serial;

/**
 * 查询字段异常 — 查询涉及的"字段维度"不合法时抛出。
 *
 * <p>典型场景:字段为空 / 非法字段名(路径穿透、非法字符)/ 排序字段非法 / 排序数量超限等。
 * 继承 {@link QueryValidationException}(参数异常),业务方可按字段维度单独捕获映射为 400。
 */
public class QueryFieldValidationException extends QueryValidationException {

    @Serial
    private static final long serialVersionUID = 1L;

    public QueryFieldValidationException(String message) {
        super(message);
    }
}
