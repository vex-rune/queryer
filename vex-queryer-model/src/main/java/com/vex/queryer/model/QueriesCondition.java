package com.vex.queryer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询条件 — 单个比较条件:field + op + value;支持嵌套谓词组合。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueriesCondition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字段名;必须为实体顶层列名
     */
    private String field;
    /**
     * 比较操作符;见 QueriesOperatorEnum
     */
    private QueriesOperatorEnum op;
    /**
     * 比较值;类型与 op 匹配
     */
    private Object value;

    /**
     * 快速构建
     * <p>
     * 三参构造便利方法;落库 + 不发事件;纯对象构造。
     *
     * @param field 字段名
     * @param op    操作符
     * @param value 值
     *
     * @return QueriesCondition 实例
     */
    public static QueriesCondition of(String field, QueriesOperatorEnum op, Object value) {
        QueriesCondition c = new QueriesCondition();
        c.setField(field);
        c.setOp(op);
        c.setValue(value);
        return c;
    }

}
