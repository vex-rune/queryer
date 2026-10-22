package com.vex.queryer.example.custom;

import com.vex.queryer.QueryFieldValidationException;
import com.vex.queryer.QueryValidationException;
import com.vex.queryer.QueriesExecutor;
import com.vex.queryer.guard.QueryGuard;
import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesCriteria;
import com.vex.queryer.model.QueriesOperatorEnum;
import com.vex.queryer.model.QueriesOrder;
import com.vex.queryer.model.QueriesPage;
import com.vex.queryer.model.QueriesPageRequest;
import com.vex.queryer.model.QueriesPredicate;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 内存存储执行器 — 演示 {@link QueriesExecutor} 抽象基类的"存储无关"能力。
 *
 * <p>不依赖 JPA / 数据库,直接在一份内存集合上实现三件套查询;复用基类自动加载的
 * 检验器({@link #guards()})、值转换器({@link #converters()})、数据域过滤
 * ({@link #dataScopeConditions()})与限制器({@link #limits()})。
 *
 * <p>这就是未来 {@code DataQueriesExecutor}(如 Spring Data MongoDB)的写法模板:
 * 存储实现只管"把谓词翻译成存储语义",其余编排全部由抽象基类保证一致。
 */
public class InMemoryQueriesExecutor<E> extends QueriesExecutor<E> {

    private final List<E> store;
    private QueriesPredicate predicate;
    private List<QueriesOrder> orders;

    public InMemoryQueriesExecutor(Collection<E> data) {
        this.store = new ArrayList<>(data);
    }

    @Override
    public List<E> page(QueriesPageRequest req) {
        if (req != null) {
            if (req.getPredicate() != null) this.predicate = req.getPredicate();
            if (req.getOrder() != null) this.orders = req.getOrder();
            if (req.getPage() != null) return page(req.getPage());
        }
        return list();
    }

    @Override
    public List<E> page(QueriesPage page) {
        List<E> rows = list();
        int size = Math.max(page.getSize(), 1);
        int pageIndex = Math.max(page.getPage(), 0);
        int from = Math.min(pageIndex * size, rows.size());
        int to = Math.min(from + size, rows.size());
        return new ArrayList<>(rows.subList(from, to));
    }

    @Override
    public List<E> list() {
        List<E> rows = new ArrayList<>(store);

        // 数据域过滤自动加入 — 与 JPA 实现同语义
        List<QueriesCondition> scope = dataScopeConditions();
        if (scope != null && !scope.isEmpty()) {
            rows.removeIf(e -> !matchAll(e, scope));
        }
        if (!isEmptyPredicate(predicate)) {
            checkScale(predicate);
            rows.removeIf(e -> !matchPredicate(e, predicate));
        }
        if (orders != null && !orders.isEmpty()) {
            if (orders.size() > limits().maxOrders()) {
                throw new QueryValidationException("排序字段数量超限: count=" + orders.size() + ", 上限=" + limits().maxOrders());
            }
            rows.sort(orderComparator());
        }
        return rows;
    }

    @Override
    public long count() {
        return list().size();
    }

    @Override
    public long count(QueriesPredicate predicate) {
        this.predicate = predicate;
        return count();
    }

    @Override
    public boolean exists(QueriesPredicate predicate) {
        return count(predicate) > 0;
    }

    // ===== 谓词匹配 =====

    private boolean matchPredicate(E entity, QueriesPredicate p) {
        boolean okAnd = true;
        if (p.getAnd() != null && !p.getAnd().isEmpty()) {
            okAnd = p.getAnd().stream().allMatch(c -> matchCriteria(entity, c));
        }
        boolean okOr = true;
        if (p.getOr() != null && !p.getOr().isEmpty()) {
            okOr = p.getOr().stream().anyMatch(c -> matchCriteria(entity, c));
        }
        return okAnd && okOr;
    }

    private boolean matchCriteria(E entity, QueriesCriteria criteria) {
        if (criteria.getCondition() != null) {
            return matchCondition(entity, criteria.getCondition());
        }
        if (criteria.getPredicate() != null) {
            return matchPredicate(entity, criteria.getPredicate());
        }
        return true;
    }

    private boolean matchCondition(E entity, QueriesCondition c) {
        if (c.getField() == null || c.getField().isBlank()) {
            throw new QueryFieldValidationException("查询条件 field 不能为空");
        }
        Class<?> fieldType = propertyType(entity.getClass(), c.getField());
        Object property = readField(entity, c.getField());
        // 校验 + in 集合归一(含自定义检验器),再按字段类型归一标量值
        guards().check(c, new QueryGuard.FieldContext(fieldType, false));
        Object value = converters().convert(c.getValue(), fieldType);

        return switch (c.getOp()) {
            case eq -> Objects.equals(property, value);
            case ne -> !Objects.equals(property, value);
            case gt -> compare(property, value) > 0;
            case gte -> compare(property, value) >= 0;
            case lt -> compare(property, value) < 0;
            case lte -> compare(property, value) <= 0;
            case like -> like(property, (String) value);
            case exp -> exp(property, (String) value);
            case in -> value instanceof Collection<?> col && col.stream().anyMatch(v -> Objects.equals(property, v));
            case isNull -> property == null;
            case isNotNull -> property != null;
        };
    }

    private boolean matchAll(E entity, List<QueriesCondition> scope) {
        return scope.stream().allMatch(c -> matchCondition(entity, c));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compare(Object property, Object value) {
        if (property instanceof Comparable p && value instanceof Comparable v) {
            return p.compareTo(v);
        }
        throw new QueryValidationException("字段值不可比较: property=" + describe(property) + ", value=" + describe(value));
    }

    private boolean like(Object property, String pattern) {
        if (!(property instanceof String s)) {
            throw new QueryValidationException("like 操作符需要 String 字段");
        }
        String regex = pattern.replace("%", ".*").replace("_", ".");
        return s.matches(regex);
    }

    private boolean exp(Object property, String pattern) {
        if (!(property instanceof String s)) {
            throw new QueryValidationException("exp 操作符需要 String 字段");
        }
        return Pattern.compile(pattern).matcher(s).find();
    }

    private String describe(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    private Comparator<E> orderComparator() {
        return (a, b) -> {
            for (QueriesOrder o : orders) {
                Class<?> type = propertyType(a.getClass(), o.getField());
                guards().checkOrder(o, new QueryGuard.FieldContext(type, false));
                int r = compare(readField(a, o.getField()), readField(b, o.getField()));
                if (r != 0) {
                    return "DESC".equalsIgnoreCase(o.getOrder().name()) ? -r : r;
                }
            }
            return 0;
        };
    }

    // ===== 字段反射读取 =====

    private Method findGetter(Class<?> type, String field) {
        String suffix = Character.toUpperCase(field.charAt(0)) + field.substring(1);
        try {
            return type.getMethod("get" + suffix);
        } catch (NoSuchMethodException ignored) {
            // 无 get 前缀,尝试 boolean 的 is 前缀
        }
        try {
            return type.getMethod("is" + suffix);
        } catch (NoSuchMethodException ignored) {
            throw new QueryFieldValidationException("实体不存在字段: " + field);
        }
    }

    private Object readField(E entity, String field) {
        try {
            if (entity.getClass().isRecord()) {
                return entity.getClass().getMethod(field).invoke(entity);
            }
            return findGetter(entity.getClass(), field).invoke(entity);
        } catch (ReflectiveOperationException ex) {
            throw new QueryFieldValidationException("读取字段失败: field=" + field);
        }
    }

    private Class<?> propertyType(Class<?> type, String field) {
        if (type.isRecord()) {
            return Arrays.stream(type.getRecordComponents())
                    .filter(rc -> rc.getName().equals(field))
                    .findFirst()
                    .orElseThrow(() -> new QueryFieldValidationException("实体不存在字段: " + field))
                    .getType();
        }
        return findGetter(type, field).getReturnType();
    }

    // ===== 规模 / 深度(与 JPA 实现同语义,复用限制器) =====

    private void checkScale(QueriesPredicate p) {
        int[] counter = {0};
        countConditions(p, counter, 1);
        if (counter[0] > limits().maxConditions()) {
            throw new QueryValidationException("查询条件数量超限: count=" + counter[0] + ", 上限=" + limits().maxConditions());
        }
    }

    private void countConditions(QueriesPredicate p, int[] counter, int depth) {
        if (p == null) {
            return;
        }
        if (depth > limits().maxDepth()) {
            throw new QueryValidationException("查询条件嵌套超过最大层数: depth=" + depth + ", 上限=" + limits().maxDepth());
        }
        countConditions(p.getAnd(), counter, depth);
        countConditions(p.getOr(), counter, depth);
    }

    private void countConditions(List<QueriesCriteria> list, int[] counter, int depth) {
        if (list == null) {
            return;
        }
        for (QueriesCriteria c : list) {
            if (c.getCondition() != null) {
                counter[0]++;
            } else if (c.getPredicate() != null) {
                countConditions(c.getPredicate(), counter, depth + 1);
            }
        }
    }

    private boolean isEmptyPredicate(QueriesPredicate p) {
        return p == null
                || ((p.getAnd() == null || p.getAnd().isEmpty())
                && (p.getOr() == null || p.getOr().isEmpty()));
    }
}
