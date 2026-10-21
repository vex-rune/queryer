package com.vex.queryer.converter;

import java.util.Locale;
import java.util.Set;

/**
 * 布尔转换器 — 支持 {@code true/false}、{@code 1/0}、{@code yes/no}。
 */
public class BooleanValueConverter implements ValueConverter {

    private static final Set<String> TRUE = Set.of("true", "1", "yes", "y", "on");
    private static final Set<String> FALSE = Set.of("false", "0", "no", "n", "off");

    @Override
    public boolean supports(Class<?> targetType) {
        return targetType == Boolean.class || targetType == boolean.class;
    }

    @Override
    public Object convert(String rawValue, Class<?> targetType) {
        String value = rawValue.trim().toLowerCase(Locale.ROOT);
        if (TRUE.contains(value)) {
            return Boolean.TRUE;
        }
        if (FALSE.contains(value)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("布尔值不合法: value=" + rawValue + ", 期望=" + TRUE + " 或 " + FALSE);
    }
}