package com.vex.queryer;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 排序条件 — 字段+方向;多列按数组顺序叠加生效。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueriesOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 排序字段名(对齐前端 fieldName 命名,fieldName 作为旧别名保留)
     */
    @JsonAlias("fieldName")
    private String field;

    /**
     * 排序方向
     */
    private QueriesOrderEnum order;

    /**
     * 排序方向枚举 — ASC 升序;DESC 降序
     */
    public enum QueriesOrderEnum {
        /**
         * 升序
         */
        ASC,
        /**
         * 降序
         */
        DESC;
    }
}
