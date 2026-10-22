package com.vex.queryer;

import com.vex.queryer.model.QueriesCondition;

import java.util.List;

/**
 * 数据域过滤器 — 在最终查询前被调用,返回需自动追加的数据域过滤条件。
 *
 * <p>用于数据权限/数据域控制(如当前主体可见范围 {@code subjectId}、租户 {@code tenantId});
 * 过滤器内部逻辑完全自定义——可以查库、查缓存、返回固定死值,由过滤器自身决定。
 *
 * <p>执行器保证:无论业务是否传入查询条件,数据域条件都会以 AND 语义自动加入
 * page / list / count / exists 全部查询入口;返回 {@code null} / 空列表 = 本次不过滤。
 *
 * <p>实现必须无状态且线程安全(执行器实例跨线程复用)。
 *
 * @param <E> 实体/文档类型;绑定执行器实体
 */
@FunctionalInterface
public interface DataScopeFilter<E> {

    /**
     * 计算数据域过滤条件;每次最终查询前被调用。
     *
     * @return 需自动追加的过滤条件;null / 空列表 = 本次不过滤
     */
    List<QueriesCondition> conditions();
}
