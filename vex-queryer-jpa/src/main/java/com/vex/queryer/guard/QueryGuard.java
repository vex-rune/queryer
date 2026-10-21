package com.vex.queryer.guard;

import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesOrder;

/**
 * 查询检验器 — 在构造 Criteria 之前拦截非法参数,避免错误参数直达 DB 触发 SQL 异常。
 *
 * <p>执行时机:字段解析通过、操作符匹配成功、值归一之后,写 SQL 之前。
 *
 * <p>与 {@link com.vex.queryer.converter.ValueConverter} 同样的可插拔模型:
 * 内置若干检验器,业务可 {@link QueryGuards#register} 追加自己的规则。
 *
 * <p>实现约定:
 * <ul>
 *     <li><b>只允许拒绝,不允许修改</b> —— 检验器无返回值,要么放行要么抛异常</li>
 *     <li>异常消息必须带业务上下文:{@code field / op / value / 约束},便于直接回给调用方</li>
 *     <li>实现必须无状态且线程安全(单例注册,跨线程复用)</li>
 * </ul>
 */
public interface QueryGuard {

    /**
     * 检验单个查询条件
     *
     * @param condition 查询条件;field/op 已通过基础校验,value 已完成类型归一
     * @param context   字段上下文;提供字段 Java 类型等元信息
     */
    void check(QueriesCondition condition, FieldContext context);

    /**
     * 检验排序项
     *
     * @param order   排序项
     * @param context 字段上下文
     */
    default void checkOrder(QueriesOrder order, FieldContext context) {
        // 默认不校验排序
    }

    /**
     * 字段上下文 — 检验器可从中取实体字段的真实类型。
     *
     * @param javaType   字段 Java 类型;非单值属性时为 null
     * @param isRelation 是否为关联路径(多值属性)
     */
    record FieldContext(Class<?> javaType, boolean isRelation) {
    }
}