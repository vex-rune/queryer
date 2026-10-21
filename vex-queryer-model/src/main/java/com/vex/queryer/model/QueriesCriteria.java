package com.vex.queryer.model;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询条件项 — condition(叶子) / predicate(嵌套) 二选一;支持 AND/OR 树形组合。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueriesCriteria implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 叶子条件;非空则忽略 predicate
     */
    @Valid
    private QueriesCondition condition;

    /**
     * 嵌套谓词;condition 为空时生效
     */
    @Valid
    private QueriesPredicate predicate;

    /**
     * 快速构建
     * <p>
     * 用叶子条件构造;落库 + 不发事件;纯对象构造。
     *
     * @param condition 叶子条件
     *
     * @return QueriesCriteria 实例
     */
    public static QueriesCriteria of(QueriesCondition condition) {
        QueriesCriteria c = new QueriesCriteria();
        c.setCondition(condition);
        return c;
    }
}
