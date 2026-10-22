package com.vex.queryer;

import java.io.Serial;

/**
 * 查询值异常 — 查询条件的"值维度"不合法时抛出。
 *
 * <p>典型场景:值类型与操作符不匹配 / 值格式非法(时间、数值、枚举、布尔)/ in 集合非法 /
 * 正则非法 / 值解析失败等。
 * 继承 {@link QueryValidationException}(参数异常),业务方可按值维度单独捕获映射为 400。
 */
public class QueryValueValidationException extends QueryValidationException {

    @Serial
    private static final long serialVersionUID = 1L;

    public QueryValueValidationException(String message) {
        super(message);
    }

    public QueryValueValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
