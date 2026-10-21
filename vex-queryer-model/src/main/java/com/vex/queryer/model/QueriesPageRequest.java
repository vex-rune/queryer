package com.vex.queryer.model;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 通用分页查询请求体 — 仅查不写;三件套 predicate/order/page;管理端接口统一 POST 此类。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueriesPageRequest implements Serializable {
    /**
     * 主体过滤字段名 — withSubjectFilter() 默认拼接 subjectId = ?
     */
    public static final String SUBJECT_ID_FIELD_NAME = "subjectId";

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 嵌套条件树;null = 无条件;支持 AND/OR 任意层级嵌套,见 QueriesPredicate
     */
    @Valid
    private QueriesPredicate predicate;
    /**
     * 排序条件列表;null/空 = 不排序;按列表顺序叠加生效
     */
    @Valid
    private List<QueriesOrder> order;
    /**
     * 分页参数;null = 全量 list();page 从 0 起始
     */
    @Valid
    private QueriesPage page;

    /**
     * 追加 AND 条件
     * <p>
     * 向 AND 列表追加叶子条件;predicate/and 为 null 时自动创建;仅改请求体内存状态,不直接落库。
     *
     * @param condition 叶子条件;field 必须为实体顶层列名
     */
    public void addAndCondition(QueriesCondition condition) {
        if (predicate == null) {
            predicate = new QueriesPredicate();
        }
        if (predicate.getAnd() == null) {
            predicate.setAnd(new ArrayList<>());
        }
        predicate.getAnd().add(QueriesCriteria.of(condition));
    }

    /**
     * 追加主体过滤
     * <p>
     * 等价在 AND 列表追加 subjectId = subjectIdSupplier.get();无上下文时为空操作;仅改内存。
     *
     * @param subjectIdSupplier 主体 ID 供应方;内部调用时传 ThreadLocal 或类似机制
     */
    public void withSubjectFilter(Optional<String> subjectIdSupplier) {
        subjectIdSupplier.ifPresent(sid ->
                addAndCondition(QueriesCondition.of(
                        SUBJECT_ID_FIELD_NAME, QueriesOperatorEnum.eq, sid)));
    }

    /**
     * 追加软删除过滤
     * <p>
     * 等价在 AND 列表追加 state != DELETED;仅改请求体内存状态,不直接落库。
     *
     * @param stateFieldName 状态字段名;默认使用 BaseEntity.STATE_FIELD_NAME
     * @param deletedState   删除状态值
     */
    public void withDeletedFilter(String stateFieldName, Object deletedState) {
        addAndCondition(QueriesCondition.of(
                stateFieldName, QueriesOperatorEnum.ne, deletedState));
    }

    /**
     * 追加状态过滤
     * <p>
     * 等价在 AND 列表追加 state = #{state};仅改请求体内存状态,不直接落库。
     *
     * @param stateFieldName 状态字段名;默认使用 BaseEntity.STATE_FIELD_NAME
     * @param state          目标状态;非 null
     */
    public void withStateFilter(String stateFieldName, Object state) {
        addAndCondition(QueriesCondition.of(
                stateFieldName, QueriesOperatorEnum.eq, state));
    }
}
