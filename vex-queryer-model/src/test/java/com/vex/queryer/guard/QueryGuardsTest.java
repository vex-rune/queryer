package com.vex.queryer.guard;

import com.vex.queryer.QueryFieldValidationException;
import com.vex.queryer.QueryValidationException;
import com.vex.queryer.QueryValueValidationException;
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
    void fieldNameWithPathSeparatorRejected_fieldException() {
        QueryFieldValidationException ex = assertThrows(QueryFieldValidationException.class,
                () -> FieldAndScaleGuard.checkFieldName("owner.secret"));
        assertTrue(ex.getMessage().contains("owner.secret"), ex.getMessage());
    }

    @Test
    void blankFieldNameRejected() {
        assertThrows(IllegalArgumentException.class, () -> FieldAndScaleGuard.checkFieldName(""));
        assertThrows(IllegalArgumentException.class, () -> FieldAndScaleGuard.checkFieldName(null));
    }

    @Test
    void fieldNameWithSpaceOrSqlFragmentRejected() {
        assertThrows(IllegalArgumentException.class, () -> FieldAndScaleGuard.checkFieldName("id; DROP TABLE"));
        assertThrows(IllegalArgumentException.class, () -> FieldAndScaleGuard.checkFieldName("1=1 OR id"));
    }

    @Test
    void legalFieldNameAllowed() {
        assertDoesNotThrow(() -> FieldAndScaleGuard.checkFieldName("code"));
        assertDoesNotThrow(() -> FieldAndScaleGuard.checkFieldName("_private2"));
    }

    @Test
    void orderCountOverLimitRejected() {
        assertDoesNotThrow(() -> FieldAndScaleGuard.checkOrderCount(3));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> FieldAndScaleGuard.checkOrderCount(11));
        assertTrue(ex.getMessage().contains("11"), ex.getMessage());
    }

    @Test
    void comparisonOperatorOnNonComparableRejected_valueException() {
        // List 不实现 Comparable,无法参与大小比较
        QueriesCondition c = QueriesCondition.of("id", QueriesOperatorEnum.gte, List.of("a"));
        QueryValueValidationException ex = assertThrows(QueryValueValidationException.class,
                () -> QueryGuards.get().check(c, STRING_CTX));
        assertTrue(ex.getMessage().contains("gte"), ex.getMessage());
    }

    @Test
    void comparisonOperatorOnStringAllowed() {
        // String 实现 Comparable,字符串字段的时间/编码序比较是合法诉求
        assertDoesNotThrow(() -> QueryGuards.get().check(
                QueriesCondition.of("code", QueriesOperatorEnum.gte, "abc"), STRING_CTX));
    }

    @Test
    void likeOnNonStringRejected() {
        QueriesCondition c = QueriesCondition.of("code", QueriesOperatorEnum.like, 123);
        assertThrows(IllegalArgumentException.class, () -> QueryGuards.get().check(c, STRING_CTX));
    }

    @Test
    void isNullWithValueRejected() {
        QueriesCondition c = QueriesCondition.of("code", QueriesOperatorEnum.isNull, "x");
        assertThrows(IllegalArgumentException.class, () -> QueryGuards.get().check(c, STRING_CTX));
    }

    @Test
    void inWithEmptyListRejected() {
        QueriesCondition c = QueriesCondition.of("code", QueriesOperatorEnum.in, List.of());
        assertThrows(IllegalArgumentException.class, () -> QueryGuards.get().check(c, STRING_CTX));
    }

    @Test
    void expWithOversizedPatternRejected() {
        QueriesCondition c = QueriesCondition.of("code", QueriesOperatorEnum.exp, "a".repeat(1001));
        assertThrows(IllegalArgumentException.class, () -> QueryGuards.get().check(c, STRING_CTX));
    }

    @Test
    void inValuesNormalizedByFieldType() {
        QueriesCondition c = QueriesCondition.of("createdAt", QueriesOperatorEnum.in,
                List.of("2026-10-21T09:23:40Z", "2026-10-22T09:23:40Z"));
        QueryGuards.get().check(c, TIME_CTX);
        @SuppressWarnings("unchecked")
        List<Object> values = (List<Object>) c.getValue();
        assertEquals(Instant.parse("2026-10-21T09:23:40Z"), values.get(0));
        assertEquals(Instant.parse("2026-10-22T09:23:40Z"), values.get(1));
    }

    @Test
    void customGuardRegistrableAndFailureThrowsValidationException() {
        QueryGuards.get().register(new QueryGuard() {
            @Override
            public void check(QueriesCondition condition, FieldContext context) {
                if ("code".equals(condition.getField())) {
                    throw new IllegalArgumentException("业务规则:禁止按 code 查询, field=" + condition.getField());
                }
            }
        });
        QueryValidationException ex = assertThrows(QueryValidationException.class,
                () -> QueryGuards.get().check(QueriesCondition.of("code", QueriesOperatorEnum.eq, "x"), STRING_CTX));
        assertTrue(ex.getMessage().contains("禁止"), ex.getMessage());
        // 不影响其他字段
        assertDoesNotThrow(() -> QueryGuards.get().check(QueriesCondition.of("name", QueriesOperatorEnum.eq, "x"), STRING_CTX));
    }
}
