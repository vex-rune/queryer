package com.vex.queryer.guard;

import com.vex.queryer.QueryValueValidationException;
import com.vex.queryer.converter.ValueConverters;
import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesOperatorEnum;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 集合元素归一检验器 — {@code in} 操作的元素逐个按字段类型转换,并限制集合规模。
 *
 * <p>JSON 反序列化后 {@code in} 的 value 是 {@code List<String>},元素同样丢失类型信息;
 * 不逐个转换会在存储上炸出 {@code operator does not exist: ... = character varying}。
 * 转换后回写列表,使下游策略拿到的是可直接比较的类型。
 */
public class InCollectionGuard implements QueryGuard {

    /// 单次 in 操作的最大元素数;超出直接拒绝,避免巨型 IN 子句拖垮执行计划
    private static final int MAX_IN_SIZE = 1000;

    @Override
    public void check(QueriesCondition condition, FieldContext context) {
        if (condition.getOp() != QueriesOperatorEnum.in) {
            return;
        }
        if (!(condition.getValue() instanceof Collection<?> raw)) {
            return;
        }
        if (raw.size() > MAX_IN_SIZE) {
            throw new QueryValueValidationException("in 集合元素过多: field=" + condition.getField()
                    + ", size=" + raw.size() + ", 上限=" + MAX_IN_SIZE);
        }
        if (context.javaType() == null) {
            return;
        }
        List<Object> converted = new ArrayList<>(raw.size());
        for (Object element : raw) {
            converted.add(ValueConverters.get().convert(element, context.javaType()));
        }
        condition.setValue(converted);
    }
}
