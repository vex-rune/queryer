package com.vex.queryer.converter;

import com.fasterxml.jackson.annotation.JsonValue;
import com.vex.queryer.QueryValueValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 值转换器注册表测试;纯内存断言,不连 DB。
 */
class ValueConvertersTest {

    private final ValueConverters registry = ValueConverters.get();

    @Test
    void time_isoWithOffset() {
        Object result = registry.convert("2026-10-21T17:23:40.991+08:00", Instant.class);
        assertEquals(Instant.parse("2026-10-21T09:23:40.991Z"), result);
    }

    @Test
    void time_isoZulu() {
        Object result = registry.convert("2026-10-21T09:23:40Z", Instant.class);
        assertEquals(Instant.parse("2026-10-21T09:23:40Z"), result);
    }

    @Test
    void time_epochMillis() {
        Object result = registry.convert("1799947220991", Instant.class);
        assertEquals(Instant.ofEpochMilli(1799947220991L), result);
    }

    @Test
    void time_wallClockParsedAsUtc() {
        Object result = registry.convert("2026-10-21 17:23:40", Instant.class);
        assertEquals(Instant.parse("2026-10-21T17:23:40Z"), result);
    }

    @Test
    void time_convertsToLocalDateAndLocalDateTime() {
        assertEquals(LocalDate.of(2026, 10, 21), registry.convert("2026-10-21T09:23:40Z", LocalDate.class));
        assertEquals(LocalDateTime.parse("2026-10-21T09:23:40"), registry.convert("2026-10-21T09:23:40Z", LocalDateTime.class));
        assertEquals(OffsetDateTime.parse("2026-10-21T09:23:40Z"), registry.convert("2026-10-21T09:23:40Z", OffsetDateTime.class));
    }

    @Test
    void time_malformedThrowsValueExceptionWithContext() {
        QueryValueValidationException ex = assertThrows(QueryValueValidationException.class,
                () -> registry.convert("not-a-date", Instant.class));
        assertTrue(ex.getMessage().contains("not-a-date"), ex.getMessage());
    }

    @Test
    void enum_byName() {
        assertEquals(DemoEnum.DRAFT, registry.convert("DRAFT", DemoEnum.class));
    }

    @Test
    void enum_byJsonValue() {
        assertEquals(DemoEnum.PUBLISHED, registry.convert("1", DemoEnum.class));
    }

    @Test
    void enum_ignoringCase() {
        assertEquals(DemoEnum.DRAFT, registry.convert("draft", DemoEnum.class));
    }

    @Test
    void enum_unmatchedThrowsValueException() {
        assertThrows(QueryValueValidationException.class, () -> registry.convert("UNKNOWN", DemoEnum.class));
    }

    @Test
    void numbersAndBooleans() {
        assertEquals(42L, registry.convert("42", Long.class));
        assertEquals(42, registry.convert("42", Integer.class));
        assertEquals(4.5d, registry.convert("4.5", Double.class));
        assertEquals(Boolean.TRUE, registry.convert("yes", Boolean.class));
        assertEquals(Boolean.FALSE, registry.convert("0", boolean.class));
    }

    @Test
    void nonStringInputAndUnregisteredTargetPassThrough() {
        Object already = 42L;
        assertSame(already, registry.convert(already, Long.class));
        assertEquals("abc", registry.convert("abc", String.class));
        // 无匹配转换器的类型原样透传
        assertEquals("abc", registry.convert("abc", UnregisteredPayload.class));
    }

    @Test
    void customConverterRegistrable() {
        registry.register(new ValueConverter() {
            @Override
            public boolean supports(Class<?> targetType) {
                return targetType == DemoPayload.class;
            }

            @Override
            public Object convert(String rawValue, Class<?> targetType) {
                return new DemoPayload(rawValue.toUpperCase(Locale.ROOT));
            }
        });
        assertEquals(new DemoPayload("HI"), registry.convert("hi", DemoPayload.class));
    }

    /// 测试用枚举;演示 @JsonValue 场景
    enum DemoEnum {
        DRAFT(0), PUBLISHED(1);

        private final int code;

        DemoEnum(int code) {
            this.code = code;
        }

        @JsonValue
        public String json() {
            return code == 0 ? "0" : "1";
        }
    }

    /// 测试用自定义类型 — 由测试用例自行注册转换器
    record DemoPayload(String value) {
    }

    /// 未注册转换器的类型 — 验证原样透传
    record UnregisteredPayload(String value) {
    }
}
