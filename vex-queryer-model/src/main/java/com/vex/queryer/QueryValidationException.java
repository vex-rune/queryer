package com.vex.queryer;

import java.io.Serial;

/**
 * 查询参数校验异常 — 查询不合法时抛出;等价业务层的"参数异常"。
 *
 * <p>按维度细分:
 * <ul>
 *     <li>字段维度 → {@link QueryFieldValidationException}</li>
 *     <li>值维度 → {@link QueryValueValidationException}</li>
 * </ul>
 * 本类为兜底类型(如用户自定义检验器抛的裸 IllegalArgumentException 包装),继承
 * {@link IllegalArgumentException} 保持兼容,业务方可按类型映射为 400 参数错误。
 */
public class QueryValidationException extends IllegalArgumentException {

    @Serial
    private static final long serialVersionUID = 1L;

    public QueryValidationException(String message) {
        super(message);
    }

    public QueryValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
