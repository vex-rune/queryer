package com.vex.queryer;

import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesCriteria;
import com.vex.queryer.model.QueriesOperatorEnum;
import com.vex.queryer.model.QueriesOrder;
import com.vex.queryer.model.QueriesPage;
import com.vex.queryer.model.QueriesPageRequest;
import com.vex.queryer.model.QueriesPredicate;
import com.vex.queryer.guard.QueryGuard;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JPA 通用查询执行器 — 基于 Jakarta Criteria;统一封装 predicate/order/page 三件套;仅查不写。
 *
 * <p>{@link QueriesExecutor} 的 JPA 实现;存储相关逻辑(CriteriaBuilder / EntityManager)
 * 全部收敛在本类,对外不暴露任何 JPA 类型;检验器与值转换器由基类构造期默认加载。
 */
public class JpaQueriesExecutor<E> extends QueriesExecutor<E> {

    /**
     * 操作符 → 谓词构建策略缓存;线程安全,类加载时一次性初始化。
     */
    private static final Map<QueriesOperatorEnum, Function<PredicateContext, Predicate>> STRATEGY_CACHE = new ConcurrentHashMap<>();

    static {
        for (QueriesOperatorEnum op : QueriesOperatorEnum.values()) {
            STRATEGY_CACHE.put(op, createStrategy(op));
        }
    }

    /**
     * 实体类型;构造期固定,不可变
     */
    private final Class<E> entityClass;
    /**
     * JPA EntityManager;由 Spring 注入,持有持久化上下文
     */
    private final EntityManager entityManager;
    /**
     * 当前查询谓词;调用 page(req)/count(predicate) 时刷新
     */
    private QueriesPredicate predicate;
    /**
     * 当前排序集合;null 表示无显式排序(DB 决定)
     */
    private List<QueriesOrder> orders;

    /**
     * 构造执行器
     * <p>
     * 使用默认限制器;惰性执行,不连接 DB;落库 + 不发事件;无副作用。
     *
     * @param entityClass   实体类型;必须为 @Entity 标注的 JPA 实体
     * @param entityManager JPA EntityManager;由 Spring 注入
     */
    public JpaQueriesExecutor(Class<E> entityClass, EntityManager entityManager) {
        this(entityClass, entityManager, QueryLimits.DEFAULT);
    }

    /**
     * 构造执行器 — 以自定义限制器覆盖默认
     * <p>
     * 惰性执行,不连接 DB;落库 + 不发事件;无副作用。
     *
     * @param entityClass   实体类型;必须为 @Entity 标注的 JPA 实体
     * @param entityManager JPA EntityManager;由 Spring 注入
     * @param limits        查询限制器;条件数/排序数/嵌套深度上限
     */
    public JpaQueriesExecutor(Class<E> entityClass, EntityManager entityManager, QueryLimits limits) {
        super(limits);
        this.entityClass = entityClass;
        this.entityManager = entityManager;
    }

    /**
     * 快速构造
     * <p>
     * 工厂方法;落库 + 不发事件;无内部状态,线程可复用。
     *
     * @param entityClass   实体类型
     * @param entityManager JPA EntityManager
     * @param <E>           实体类型
     *
     * @return 同语义实例
     */
    public static <E> JpaQueriesExecutor<E> of(Class<E> entityClass, EntityManager entityManager) {
        return new JpaQueriesExecutor<>(entityClass, entityManager);
    }

    /**
     * 快速构造 — 以自定义限制器覆盖默认
     *
     * @param entityClass   实体类型
     * @param entityManager JPA EntityManager
     * @param limits        查询限制器
     * @param <E>           实体类型
     *
     * @return 同语义实例
     */
    public static <E> JpaQueriesExecutor<E> of(Class<E> entityClass, EntityManager entityManager, QueryLimits limits) {
        return new JpaQueriesExecutor<>(entityClass, entityManager, limits);
    }

    private static Function<PredicateContext, Predicate> createStrategy(QueriesOperatorEnum op) {
        return switch (op) {
            case eq -> ctx -> ctx.builder.equal(ctx.path, ctx.value);
            case ne -> ctx -> ctx.builder.notEqual(ctx.path, ctx.value);
            case gt -> ctx -> {
                requireComparable(ctx.value, ctx.operator);
                @SuppressWarnings({"unchecked", "rawtypes"})
                Class<Comparable> cls = (Class<Comparable>) ctx.value.getClass();
                @SuppressWarnings({"unchecked", "rawtypes"})
                Comparable v = (Comparable) ctx.value;
                return ctx.builder.greaterThan(ctx.path.as(cls), v);
            };
            case gte -> ctx -> {
                requireComparable(ctx.value, ctx.operator);
                @SuppressWarnings({"unchecked", "rawtypes"})
                Class<Comparable> cls = (Class<Comparable>) ctx.value.getClass();
                @SuppressWarnings({"unchecked", "rawtypes"})
                Comparable v = (Comparable) ctx.value;
                return ctx.builder.greaterThanOrEqualTo(ctx.path.as(cls), v);
            };
            case lt -> ctx -> {
                requireComparable(ctx.value, ctx.operator);
                @SuppressWarnings({"unchecked", "rawtypes"})
                Class<Comparable> cls = (Class<Comparable>) ctx.value.getClass();
                @SuppressWarnings({"unchecked", "rawtypes"})
                Comparable v = (Comparable) ctx.value;
                return ctx.builder.lessThan(ctx.path.as(cls), v);
            };
            case lte -> ctx -> {
                requireComparable(ctx.value, ctx.operator);
                @SuppressWarnings({"unchecked", "rawtypes"})
                Class<Comparable> cls = (Class<Comparable>) ctx.value.getClass();
                @SuppressWarnings({"unchecked", "rawtypes"})
                Comparable v = (Comparable) ctx.value;
                return ctx.builder.lessThanOrEqualTo(ctx.path.as(cls), v);
            };
            case like -> ctx -> {
                requireString(ctx.value, ctx.operator);
                return ctx.builder.like(ctx.path.as(String.class), (String) ctx.value);
            };
            case exp -> ctx -> {
                requireString(ctx.value, ctx.operator);
                // PostgreSQL 15+ 方言:regexp_like;其他数据库需换对应方言函数
                return ctx.builder.isTrue(ctx.builder.function("regexp_like",
                        Boolean.class, ctx.path.as(String.class), ctx.builder.literal(ctx.value)));
            };
            case in -> ctx -> {
                requireCollection(ctx.value, ctx.operator);
                return ctx.path.in((Collection<?>) ctx.value);
            };
            case isNull -> ctx -> {
                requireNull(ctx.value, ctx.operator);
                return ctx.builder.isNull(ctx.path);
            };
            case isNotNull -> ctx -> {
                requireNull(ctx.value, ctx.operator);
                return ctx.builder.isNotNull(ctx.path);
            };
        };
    }

    // ====== 策略实现 ======

    private static void requireComparable(Object value, QueriesOperatorEnum op) {
        if (value == null || !(value instanceof Comparable)) {
            throw new QueryValueValidationException(op + " 操作符需要 Comparable 类型,实际=" + (value == null ? "null" : value.getClass().getName()));
        }
    }

    private static void requireString(Object value, QueriesOperatorEnum op) {
        if (!(value instanceof String)) {
            throw new QueryValueValidationException(op + " 操作符需要 String 类型,实际=" + (value == null ? "null" : value.getClass().getName()));
        }
    }

    private static void requireNull(Object value, QueriesOperatorEnum op) {
        if (value != null) {
            throw new QueryValueValidationException(op + " 操作符不需要传入值");
        }
    }

    private static void requireCollection(Object value, QueriesOperatorEnum op) {
        if (!(value instanceof Collection)) {
            throw new QueryValueValidationException(op + " 操作符需要集合类型,实际=" + (value == null ? "null" : value.getClass().getName()));
        }
    }

    /**
     * 分页查询(请求体)
     * <p>
     * req.predicate/order 暂存到本实例,后续 count() 可复用;req.page 为空降级 list();仅查不写。
     *
     * @param req 分页查询请求;null 时直接走 list()
     *
     * @return 当前页数据行;可能为空列表
     */
    @Override
    public List<E> page(QueriesPageRequest req) {
        if (req != null) {
            if (req.getPredicate() != null) this.predicate = req.getPredicate();
            if (req.getOrder() != null) this.orders = req.getOrder();
            if (req.getPage() != null) return page(req.getPage());
        }
        return list();
    }

    // ====== 查询入口 ======

    /**
     * 分页查询(页码)
     * <p>
     * 复用实例 predicate/排序;page&lt;0 自动夹到 0,size&lt;1 自动夹到 1;仅查不写。
     *
     * @param page 分页参数;page 从 0 起始
     *
     * @return 当前页数据行
     */
    @Override
    public List<E> page(QueriesPage page) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<E> query = buildSelectQuery(builder);

        int pageIndex = Math.max(page.getPage(), 0);
        int size = Math.max(page.getSize(), 1);
        TypedQuery<E> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult(pageIndex * size);
        typedQuery.setMaxResults(size);
        return typedQuery.getResultList();
    }

    /**
     * 全量列表
     * <p>
     * 无分页、无 LIMIT;大数据量请改用 page(QueriesPage);仅查不写。
     *
     * @return 全量数据行;可能为空列表
     */
    @Override
    public List<E> list() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<E> query = buildSelectQuery(builder);

        TypedQuery<E> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

    /**
     * 统计总数
     * <p>
     * SELECT COUNT(id);要求实体有 id 字段;复用实例 predicate;仅查不写。
     *
     * @return 命中行数;无命中返回 0
     */
    @Override
    public long count() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<E> root = query.from(entityClass);
        applyWhereClause(query, root, builder);
        query.select(builder.count(root.get("id")));
        return entityManager.createQuery(query).getSingleResult();
    }

    /**
     * 统计(临时谓词)
     * <p>
     * 用临时谓词统计;覆盖实例 predicate,不影响排序;仅查不写。
     *
     * @param predicate 查询谓词;null 表示不过滤
     *
     * @return 命中行数
     */
    @Override
    public long count(QueriesPredicate predicate) {
        this.predicate = predicate;
        return count();
    }

    /**
     * 判断存在
     * <p>
     * COUNT(id) > 0 判定;性能等价 count(QueriesPredicate);仅查不写。
     *
     * @param predicate 查询谓词;null 时退化为查表是否存在
     *
     * @return true=至少一行
     */
    @Override
    public boolean exists(QueriesPredicate predicate) {
        return count(predicate) > 0;
    }

    private CriteriaQuery<E> buildSelectQuery(CriteriaBuilder builder) {
        CriteriaQuery<E> query = builder.createQuery(entityClass);
        Root<E> root = query.from(entityClass);
        applyWhereClause(query, root, builder);
        applyOrdering(query, root, builder);
        return query;
    }

    // ====== 构建内部 ======

    private void applyWhereClause(CriteriaQuery<?> query, Root<?> root, CriteriaBuilder builder) {
        List<Predicate> predicates = new ArrayList<>();
        // 数据域过滤自动加入 — 无论业务是否传条件,最终查询前都生效(AND 语义)
        List<QueriesCondition> scope = dataScopeConditions();
        if (scope != null && !scope.isEmpty()) {
            predicates.add(buildPredicateFromCriteria(
                    scope.stream().map(QueriesCriteria::of).toList(), true, root, builder));
        }
        if (!isEmptyPredicate(predicate)) {
            checkConditionScale(predicate);
            if (predicate.getAnd() != null && !predicate.getAnd().isEmpty()) {
                predicates.add(buildPredicateFromCriteria(predicate.getAnd(), true, root, builder));
            }
            if (predicate.getOr() != null && !predicate.getOr().isEmpty()) {
                predicates.add(buildPredicateFromCriteria(predicate.getOr(), false, root, builder));
            }
        }
        if (predicates.isEmpty()) {
            query.where();
        } else if (predicates.size() == 1) {
            query.where(predicates.get(0));
        } else {
            query.where(builder.and(predicates.toArray(new Predicate[0])));
        }
    }

    private Predicate buildPredicateFromCriteria(List<QueriesCriteria> criteriaList,
                                                  boolean isAnd,
                                                  Root<?> root,
                                                  CriteriaBuilder builder) {
        List<Predicate> predicates = new ArrayList<>();
        for (QueriesCriteria c : criteriaList) {
            if (c.getCondition() != null) {
                predicates.add(buildPredicateFromCondition(c.getCondition(), root, builder));
            } else if (c.getPredicate() != null) {
                List<QueriesCriteria> and = c.getPredicate().getAnd();
                if (and != null && !and.isEmpty()) {
                    predicates.add(buildPredicateFromCriteria(and, true, root, builder));
                }
                List<QueriesCriteria> or = c.getPredicate().getOr();
                if (or != null && !or.isEmpty()) {
                    predicates.add(buildPredicateFromCriteria(or, false, root, builder));
                }
            }
        }
        if (predicates.isEmpty()) return builder.conjunction();
        return isAnd
                ? builder.and(predicates.toArray(new Predicate[0]))
                : builder.or(predicates.toArray(new Predicate[0]));
    }

    private Predicate buildPredicateFromCondition(QueriesCondition c, Root<?> root, CriteriaBuilder builder) {
        if (c.getField() == null || c.getField().isBlank()) {
            throw new QueryFieldValidationException("查询条件 field 不能为空");
        }
        // 校验字段是否存在于实体中,防止通过反射访问敏感字段
        String field = c.getField();
        Path<?> path;
        try {
            path = root.get(field);
        } catch (IllegalArgumentException e) {
            throw new QueryFieldValidationException("不合法的查询字段: " + field);
        }
        Function<PredicateContext, Predicate> strategy = STRATEGY_CACHE.get(c.getOp());
        if (strategy == null) {
            throw new QueryValidationException("不支持的操作符: " + c.getOp());
        }
        // 值类型归一后再交检验器,保证检验器看到的是目标类型的值
        c.setValue(resolveValue(c.getValue(), path));
        guards().check(c, fieldContext(path));
        return strategy.apply(new PredicateContext(builder, path, c.getValue(), c.getOp()));
    }

    /**
     * 字段上下文 — 从 Criteria Path 提取实体属性的 Java 类型供检验器使用。
     * <p>
     * 必须用 {@code path.getJavaType()} 而非 {@code instanceof SingularAttribute} 判断:
     * Hibernate 6 下 {@code root.get(field)} 返回的是 SQM 层的 {@code SingularAttributePath},
     * 并非 Metamodel 层的 {@code SingularAttribute} 实例,instanceof 判断恒为 false。
     *
     * @param path 字段路径
     *
     * @return 字段上下文
     */
    private QueryGuard.FieldContext fieldContext(Path<?> path) {
        return new QueryGuard.FieldContext(path.getJavaType(), false);
    }

    /**
     * 值类型归一 — JSON 反序列化残留的字符串按字段真实类型转换。
     * <p>
     * {@code QueriesCondition.value} 声明为 {@code Object},Jackson 无法识别 {@code Instant} / 枚举等类型,
     * 只保留字符串;不转换直接与 {@code timestamptz} 列比较会触发 SQL 语法错
     * ({@code operator does not exist: timestamp with time zone >= character varying})。
     * 转换器可插拔,见 {@link com.vex.queryer.converter.ValueConverters}。
     *
     * @param rawValue 原始值
     * @param path     字段路径;用于取实体属性的 Java 类型
     *
     * @return 目标类型的值;无匹配转换器时原样返回
     */
    private Object resolveValue(Object rawValue, Path<?> path) {
        if (!(rawValue instanceof String)) {
            return rawValue;
        }
        return converters().convert(rawValue, path.getJavaType());
    }

    private void applyOrdering(CriteriaQuery<E> query, Root<E> root, CriteriaBuilder builder) {
        if (orders == null || orders.isEmpty()) return;
        if (orders.size() > limits().maxOrders()) {
            throw new QueryValidationException("排序字段数量超限: count=" + orders.size() + ", 上限=" + limits().maxOrders());
        }
        List<Order> orderList = orders.stream()
                .map(o -> {
                    // 校验排序字段是否存在于实体中
                    Path<?> path;
                    try {
                        path = root.get(o.getField());
                    } catch (IllegalArgumentException e) {
                        throw new QueryFieldValidationException("不合法的排序字段: " + o.getField());
                    }
                    guards().checkOrder(o, fieldContext(path));
                    return "DESC".equalsIgnoreCase(o.getOrder().name())
                            ? builder.desc(path)
                            : builder.asc(path);
                })
                .collect(Collectors.toList());
        query.orderBy(orderList);
    }

    private boolean isEmptyPredicate(QueriesPredicate p) {
        return p == null
                || ((p.getAnd() == null || p.getAnd().isEmpty())
                && (p.getOr() == null || p.getOr().isEmpty()));
    }

    /**
     * 谓词规模校验 — 递归统计 condition 节点与嵌套深度,超限直接拒绝。
     * <p>
     * 防止构造出巨型 WHERE 子句拖垮 DB 执行计划生成器。
     * 根谓词计为第 1 层,每嵌套一层 +1;超过限制器 maxDepth 拒绝。
     *
     * @param p 查询谓词
     */
    private void checkConditionScale(QueriesPredicate p) {
        if (p == null) {
            return;
        }
        int[] counter = {0};
        countConditions(p, counter, 1);
        if (counter[0] > limits().maxConditions()) {
            throw new QueryValidationException("查询条件数量超限: count=" + counter[0]
                    + ", 上限=" + limits().maxConditions());
        }
    }

    private void countConditions(QueriesPredicate p, int[] counter, int depth) {
        if (p == null) {
            return;
        }
        if (depth > limits().maxDepth()) {
            throw new QueryValidationException("查询条件嵌套超过最大层数: depth=" + depth
                    + ", 上限=" + limits().maxDepth());
        }
        countConditions(p.getAnd(), counter, depth);
        countConditions(p.getOr(), counter, depth);
    }

    private void countConditions(List<QueriesCriteria> criteriaList, int[] counter, int depth) {
        if (criteriaList == null) {
            return;
        }
        for (QueriesCriteria c : criteriaList) {
            if (c.getCondition() != null) {
                counter[0]++;
                if (counter[0] > limits().maxConditions()) {
                    return;
                }
            } else if (c.getPredicate() != null) {
                countConditions(c.getPredicate(), counter, depth + 1);
            }
        }
    }

    /**
     * 谓词构建参数 — CriteriaBuilder + 字段路径 + 值 + 操作符;仅供操作符策略闭包使用。
     */
    private record PredicateContext(CriteriaBuilder builder, Path<?> path, Object value, QueriesOperatorEnum operator) {
    }
}
