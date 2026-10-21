package com.vex.queryer.converter;

import com.fasterxml.jackson.annotation.JsonValue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

/**
 * 枚举转换器 — 任意枚举类型通用。
 *
 * <p>匹配优先级:{@code @JsonValue} 标注值 → {@code name()} → 大写/小写容错;
 * 全部不命中时抛异常,由调用方转 400。
 */
public class EnumValueConverter implements ValueConverter {

    @Override
    public boolean supports(Class<?> targetType) {
        return targetType.isEnum();
    }

    @Override
    public Object convert(String rawValue, Class<?> targetType) {
        String value = rawValue.trim();
        // 优先 @JsonValue 外部值 — 遍历常量逐个比对,容忍枚举自定义对外值
        Method jsonValueMethod = findJsonValueMethod(targetType);
        if (jsonValueMethod != null) {
            for (Object constant : targetType.getEnumConstants()) {
                try {
                    if (value.equals(String.valueOf(jsonValueMethod.invoke(constant)))) {
                        return constant;
                    }
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException("枚举 @JsonValue 方法调用失败: enum=" + targetType.getName(), ex);
                }
            }
        }
        // 兜底 name 匹配 — 忽略大小写与 - / 空格 差异
        String normalized = normalize(value);
        for (Object constant : targetType.getEnumConstants()) {
            if (normalize(((Enum<?>) constant).name()).equals(normalized)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("枚举值不匹配: value=" + rawValue
                + ", 期望=" + Arrays.toString(targetType.getEnumConstants()));
    }

    /// 取无参且返回 String 的 @JsonValue 方法;无则返回 null
    private Method findJsonValueMethod(Class<?> enumClass) {
        for (Method method : enumClass.getMethods()) {
            if (method.getParameterCount() == 0
                    && method.isAnnotationPresent(JsonValue.class)
                    && method.getReturnType() == String.class) {
                return method;
            }
        }
        return null;
    }

    /// 归一化 — 统一大写,并把 - / 空格 视作下划线
    private String normalize(String value) {
        return value.replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }
}