package com.vex.queryer.converter;

import com.vex.queryer.QueryValueValidationException;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 数值类型转换器 — Long / Integer / Short / Byte / Double / Float / BigDecimal / BigInteger。
 */
public class NumberValueConverter implements ValueConverter {

    @Override
    public boolean supports(Class<?> targetType) {
        return targetType == Long.class || targetType == long.class
                || targetType == Integer.class || targetType == int.class
                || targetType == Short.class || targetType == short.class
                || targetType == Byte.class || targetType == byte.class
                || targetType == Double.class || targetType == double.class
                || targetType == Float.class || targetType == float.class
                || targetType == BigDecimal.class
                || targetType == BigInteger.class;
    }

    @Override
    public Object convert(String rawValue, Class<?> targetType) {
        String value = rawValue.trim();
        try {
            if (targetType == Long.class || targetType == long.class) {
                return Long.parseLong(value);
            }
            if (targetType == Integer.class || targetType == int.class) {
                return Integer.parseInt(value);
            }
            if (targetType == Short.class || targetType == short.class) {
                return Short.parseShort(value);
            }
            if (targetType == Byte.class || targetType == byte.class) {
                return Byte.parseByte(value);
            }
            if (targetType == Double.class || targetType == double.class) {
                return Double.parseDouble(value);
            }
            if (targetType == Float.class || targetType == float.class) {
                return Float.parseFloat(value);
            }
            if (targetType == BigDecimal.class) {
                return new BigDecimal(value);
            }
            return new BigInteger(value);
        } catch (NumberFormatException ex) {
            throw new QueryValueValidationException("数值格式不合法: value=" + rawValue + ", 期望=" + targetType.getSimpleName(), ex);
        }
    }
}
