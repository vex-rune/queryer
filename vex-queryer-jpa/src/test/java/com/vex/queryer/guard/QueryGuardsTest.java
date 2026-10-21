package com.vex.queryer.guard;

import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesOperatorEnum;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 查询检验器测试;纯内存断言,不连 DB。
 */
class QueryGuardsTest {

    private static final QueryGuard.FieldContext STRING_CTX = new QueryGuard.FieldContext(String.class, false);
    private static final QueryGuard.FieldContext TIME_CTX = new QueryGuard.FieldContext(Instant.class, false);

    @Test
    void 字段名含路径分隔符被拒绝() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> FieldAndScaleGuard.checkFieldName("owner.secret"));
        assertTrue(ex.getMessage().contains("owner.secret"), ex.getMessage());
    }

    @Test
    void 字段名为空被拒绝() {
        assertThrows(IllegalArgumentException.class, () -> FieldAndScaleGuard.checkFieldName(""));
        assertThrows(IllegalArgumentException.class, () -> FieldAndScaleGuard.checkFieldName(null));
    }

    @Test
    void 字段名含空格或SQL片段被拒绝() {
        assertThrows(IllegalArgumentException.class, () -> FieldAndScaleGuard.checkFieldName("id; DROP TABLE"));
        assertThrows(IllegalArgumentException.class, () -> FieldAndScaleGuard.checkFieldName("1=1 OR id"));
    }

    @Test
    void 合法字段名放行() {
        assertDoesNotThrow(() -> FieldAndScaleGuard.checkFieldName("code"));
        assertDoesNotThrow(() -> FieldAndScaleGuard.checkFieldName("_private2"));
    }

    @Test
    void 排序数量超限被拒绝() {
        assertDoesNotThrow(() -> FieldAndScaleGuard.checkOrderCount(3));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> FieldAndScaleGuard.checkOrderCount(11));
        assertTrue(ex.getMessage().contains("11"), ex.getMessage());
    }

    @Test
    void 比较操作符遇非Comparable被拒绝() {
        // List 不实现 Comparable,无法参与大小比较
        QueriesCondition c = QueriesCondition.of("id", QueriesOperatorEnum.gte, List.of("a"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> QueryGuards.get().check(c, STRING_CTX));
        assertTrue(ex.getMessage().contains("gte"), ex.getMessage());
    }

    @Test
    void 比较操作符遇字符串可放行() {
        // String 实现 Comparable,字符串字段的时间/编码序比较是合法诉求
        assertDoesNotThrow(() -> QueryGuards.get().check(
                QueriesCondition.of("code", QueriesOperatorEnum.gte, "abc"), STRING_CTX));
    }

    @Test
    void like遇非字符串被拒绝() {
        QueriesCondition c = QueriesCondition.of("code", QueriesOperatorEnum.like, 123);
        assertThrows(IllegalArgumentException.class, () -> QueryGuards.get().check(c, STRING_CTX));
    }

    @Test
    void isNull带值被拒绝() {
        QueriesCondition c = QueriesCondition.of("code", QueriesOperatorEnum.isNull, "x");
        assertThrows(IllegalArgumentException.class, () -> QueryGuards.get().check(c, STRING_CTX));
    }

    @Test
    void in空集合被拒绝() {
        QueriesCondition c = QueriesCondition.of("code", QueriesOperatorEnum.in, List.of());
        assertThrows(IllegalArgumentException.class, () -> QueryGuards.get().check(c, STRING_CTX));
    }

    @Test
    void exp正则过长被拒绝() {
        QueriesCondition c = QueriesCondition.of("code", QueriesOperatorEnum.exp, "a".repeat(1001));
        assertThrows(IllegalArgumentException.class, () -> QueryGuards.get().check(c, STRING_CTX));
    }

    @Test
    void in集合元素按字段类型归一() {
        QueriesCondition c = QueriesCondition.of("createdAt", QueriesOperatorEnum.in,
                List.of("2026-10-21T09:23:40Z", "2026-10-22T09:23:40Z"));
        QueryGuards.get().check(c, TIME_CTX);
        @SuppressWarnings("unchecked")
        List<Object> values = (List<Object>) c.getValue();
        assertEquals(Instant.parse("2026-10-21T09:23:40Z"), values.get(0));
        assertEquals(Instant.parse("2026-10-22T09:23:40Z"), values.get(1));
    }

    @Test
    void 自定义检验器可注册() {
        QueryGuards.get().register(new QueryGuard() {
            @Override
            public void check(QueriesCondition condition, FieldContext context) {
                if ("code".equals(condition.getField())) {
                    throw new IllegalArgumentException("业务规则:禁止按 code 查询, field=" + condition.getField());
                }
            }
        });
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> QueryGuards.get().check(QueriesCondition.of("code", QueriesOperatorEnum.eq, "x"), STRING_CTX));
        assertTrue(ex.getMessage().contains("禁止"), ex.getMessage());
        // 不影响其他字段
        assertDoesNotThrow(() -> QueryGuards.get().check(QueriesCondition.of("name", QueriesOperatorEnum.eq, "x"), STRING_CTX));
    }
}