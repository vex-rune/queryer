package com.vex.queryer.guard;

import com.vex.queryer.QueryValidationException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 查询检验器注册表 — 天生单例;构造器自带内置检验器,业务可在运行时追加自定义实现。
 *
 * <p>内置:操作符契约 / in 集合归一 / 字段与规模。与 {@link com.vex.queryer.converter.ValueConverters}
 * 同一套可插拔模型,按注册顺序执行,任一检验器抛异常即拒绝该次查询。
 *
 * <p>校验失败(检验器抛 {@link IllegalArgumentException})统一包装为
 * {@link QueryValidationException}(参数异常),业务方按参数异常统一捕获。
 *
 * <p>用法:
 * <pre>{@code
 * QueryGuards.get().register((condition, context) -> {
 *     if ("code".equals(condition.getField()) && condition.getOp() == eq) {
 *         throw new IllegalArgumentException("禁止按 code 精确查询");
 *     }
 * });
 * }</pre>
 */
public final class QueryGuards {

    private static final QueryGuards INSTANCE = new QueryGuards();

    /**
     * 全局单例;无状态共享,线程安全。
     */
    private final List<QueryGuard> guards = new CopyOnWriteArrayList<>();

    private QueryGuards() {
        register(new FieldAndScaleGuard());
        register(new OperatorContractGuard());
        register(new InCollectionGuard());
    }

    /**
     * 取全局单例
     *
     * @return 共享实例;进程内唯一
     */
    public static QueryGuards get() {
        return INSTANCE;
    }

    /**
     * 注册自定义检验器
     * <p>
     * 追加到列表末尾;后注册者在内置检验器之后执行,不能绕过内置约束。
     *
     * @param guard 自定义检验器;必须无状态且线程安全
     *
     * @return this;支持链式调用
     */
    public QueryGuards register(QueryGuard guard) {
        guards.add(guard);
        return this;
    }

    /**
     * 执行全部检验
     * <p>
     * 任一检验器抛 {@link IllegalArgumentException} 即视为校验不通过,包装为
     * {@link QueryValidationException} 抛出;已是该类型则原样透传。
     *
     * @param condition 查询条件
     * @param context   字段上下文
     */
    public void check(com.vex.queryer.model.QueriesCondition condition, QueryGuard.FieldContext context) {
        for (QueryGuard guard : guards) {
            try {
                guard.check(condition, context);
            } catch (IllegalArgumentException ex) {
                throw asQueryValidation(ex);
            }
        }
    }

    /**
     * 执行排序检验
     *
     * @param order   排序项
     * @param context 字段上下文
     */
    public void checkOrder(com.vex.queryer.model.QueriesOrder order, QueryGuard.FieldContext context) {
        for (QueryGuard guard : guards) {
            try {
                guard.checkOrder(order, context);
            } catch (IllegalArgumentException ex) {
                throw asQueryValidation(ex);
            }
        }
    }

    /// 统一转为参数异常;已是 QueryValidationException 则原样返回,避免重复包装
    private QueryValidationException asQueryValidation(IllegalArgumentException ex) {
        if (ex instanceof QueryValidationException qve) {
            return qve;
        }
        return new QueryValidationException(ex.getMessage(), ex);
    }
}
