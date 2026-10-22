package com.vex.queryer;

import com.vex.queryer.converter.ValueConverter;
import com.vex.queryer.converter.ValueConverters;
import com.vex.queryer.guard.QueryGuard;
import com.vex.queryer.guard.QueryGuards;
import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesPage;
import com.vex.queryer.model.QueriesPageRequest;
import com.vex.queryer.model.QueriesPredicate;

import java.util.List;

/**
 * 通用查询执行器 — 存储无关的统一查询契约;仅查不写。
 *
 * <p>抽象出 predicate/order/page 三件套的查询入口,让上层业务面向本类编程;
 * 存储实现可替换:当前基于 Jakarta Criteria 的 JPA 实现,后续可扩展
 * Spring Data MongoDB 等存储(如 {@code DataQueriesExecutor})。
 *
 * <p>构造期自动加载默认配置好的查询检验器({@link QueryGuards})与查询值转换器
 * ({@link ValueConverters});业务可经 {@link #registerGuard} / {@link #registerConverter}
 * 追加自定义规则(进程内全局生效,影响所有执行器实例)。
 *
 * <p>数据域控制:可经 {@link #dataScope} 注入一个 {@link DataScopeFilter},其返回的过滤条件
 * 会在最终查询前以 AND 语义自动加入全部查询入口(page / list / count / exists)。
 *
 * <p>查询规模限制:经 {@link QueryLimits} 约束条件数 / 排序数 / 谓词嵌套深度;
 * 默认 {@link QueryLimits#DEFAULT}(嵌套 3 层),子类构造时可传入自定义限制覆盖。
 *
 * <p>校验失败统一抛 {@link QueryValidationException}(参数异常),业务方可捕获映射为 400。
 *
 * @param <E> 实体/文档类型
 */
public abstract class QueriesExecutor<E> {

    /**
     * 查询检验器注册表 — 默认加载内置检验器(字段与规模/操作符契约/in 集合归一)
     */
    private final QueryGuards guards;

    /**
     * 查询值转换器注册表 — 默认加载内置转换器(时间/枚举/数值/布尔)
     */
    private final ValueConverters converters;

    /**
     * 数据域过滤器 — 默认不注入(不过滤);子类在最终查询前经 {@link #dataScopeConditions()} 取条件
     */
    private DataScopeFilter<E> dataScopeFilter;

    /**
     * 查询限制器 — 条件数/排序数/嵌套深度上限;默认 {@link QueryLimits#DEFAULT},构造可覆盖
     */
    private final QueryLimits limits;

    /**
     * 默认构造 — 使用 {@link QueryLimits#DEFAULT} 限制;加载默认配置好的检验器与转换器;惰性,无副作用。
     */
    protected QueriesExecutor() {
        this(QueryLimits.DEFAULT);
    }

    /**
     * 构造 — 以自定义限制器覆盖默认;其余同默认构造。
     *
     * @param limits 查询限制器;各上限必须为正数
     */
    protected QueriesExecutor(QueryLimits limits) {
        this.guards = QueryGuards.get();
        this.converters = ValueConverters.get();
        this.limits = limits;
    }

    /**
     * 注册自定义查询检验器
     * <p>
     * 追加到内置检验器之后执行;进程内全局生效,不能绕过内置约束。
     *
     * @param guard 自定义检验器;必须无状态且线程安全
     *
     * @return this;支持链式调用
     */
    public final QueriesExecutor<E> registerGuard(QueryGuard guard) {
        guards.register(guard);
        return this;
    }

    /**
     * 注册自定义查询值转换器
     * <p>
     * 追加到内置转换器之后匹配;进程内全局生效,内置类型仍按先注册者胜出。
     *
     * @param converter 自定义转换器;必须无状态且线程安全
     *
     * @return this;支持链式调用
     */
    public final QueriesExecutor<E> registerConverter(ValueConverter converter) {
        converters.register(converter);
        return this;
    }

    /**
     * 注入数据域过滤器
     * <p>
     * 单个执行器实例只允许一个数据域过滤器;重复调用覆盖前值。
     * 过滤器返回的条件会以 AND 语义自动加入 page / list / count / exists 全部查询入口。
     *
     * @param filter 数据域过滤器;null 表示清除过滤(恢复不过滤)
     *
     * @return this;支持链式调用
     */
    public final QueriesExecutor<E> dataScope(DataScopeFilter<E> filter) {
        this.dataScopeFilter = filter;
        return this;
    }

    /**
     * 解析数据域过滤条件
     * <p>
     * 子类必须在最终查询前调用本方法,并把返回条件以 AND 语义加入查询;
     * 每次调用都会重新执行过滤器,支持查库/查缓存/固定死值等动态逻辑。
     *
     * @return 数据域过滤条件;null / 空 = 本次不过滤
     */
    protected final List<QueriesCondition> dataScopeConditions() {
        return dataScopeFilter == null ? null : dataScopeFilter.conditions();
    }

    /**
     * 查询限制器
     *
     * @return 当前限制器;条件数/排序数/嵌套深度上限
     */
    protected final QueryLimits limits() {
        return limits;
    }

    /**
     * 查询检验器注册表
     *
     * @return 默认检验器注册表;业务追加的规则在此生效
     */
    protected final QueryGuards guards() {
        return guards;
    }

    /**
     * 查询值转换器注册表
     *
     * @return 默认转换器注册表;业务追加的转换器在此生效
     */
    protected final ValueConverters converters() {
        return converters;
    }

    /**
     * 分页查询(请求体)
     * <p>
     * 三件套 predicate/order/page 整体入参;page 为空时降级全量 list()。
     *
     * @param req 分页查询请求;null 时直接走 list()
     *
     * @return 当前页数据行;可能为空列表
     */
    public abstract List<E> page(QueriesPageRequest req);

    /**
     * 分页查询(页码)
     * <p>
     * 复用已暂存的 predicate/排序;page&lt;0 自动夹到 0,size&lt;1 自动夹到 1。
     *
     * @param page 分页参数;page 从 0 起始
     *
     * @return 当前页数据行
     */
    public abstract List<E> page(QueriesPage page);

    /**
     * 全量列表
     * <p>
     * 无分页、无 LIMIT;大数据量请改用 page(QueriesPage)。
     *
     * @return 全量数据行;可能为空列表
     */
    public abstract List<E> list();

    /**
     * 统计总数
     * <p>
     * 复用已暂存的 predicate;命中行数。
     *
     * @return 命中行数;无命中返回 0
     */
    public abstract long count();

    /**
     * 统计(临时谓词)
     * <p>
     * 用临时谓词统计;覆盖已暂存 predicate,不影响排序。
     *
     * @param predicate 查询谓词;null 表示不过滤
     *
     * @return 命中行数
     */
    public abstract long count(QueriesPredicate predicate);

    /**
     * 判断存在
     * <p>
     * 命中行数 &gt; 0 判定;性能等价 count(QueriesPredicate)。
     *
     * @param predicate 查询谓词;null 时退化为查存储是否存在
     *
     * @return true=至少一行
     */
    public abstract boolean exists(QueriesPredicate predicate);
}
