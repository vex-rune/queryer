package com.vex.queryer.converter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 值转换器注册表 — 天生单例;构造器自带内置转换器,业务可在运行时追加自定义实现。
 *
 * <p>内置:时间 / 枚举 / 数值 / 布尔。业务自定义类型(密码哈希、地理位置等)实现 {@link ValueConverter}
 * 后调 {@link #register} 注册即可,按注册顺序匹配,先注册者优先。
 *
 * <p>用法:
 * <pre>{@code
 * ValueConverters.get().register(new MyTypeConverter());
 * Object typed = ValueConverters.get().convert("2026-10-21T17:23:40+08:00", Instant.class);
 * }</pre>
 */
public final class ValueConverters {

    private static final ValueConverters INSTANCE = new ValueConverters();

    /**
     * 全局单例;无状态共享,线程安全。
     */
    private final List<ValueConverter> converters = new CopyOnWriteArrayList<>();

    private ValueConverters() {
        register(new TemporalValueConverter());
        register(new EnumValueConverter());
        register(new NumberValueConverter());
        register(new BooleanValueConverter());
    }

    /**
     * 取全局单例
     *
     * @return 共享实例;进程内唯一
     */
    public static ValueConverters get() {
        return INSTANCE;
    }

    /**
     * 注册自定义转换器
     * <p>
     * 追加到列表末尾;后注册的类型若与内置转换器冲突,按先注册者胜出,故内置类型不会被覆盖。
     *
     * @param converter 自定义转换器;必须无状态且线程安全
     *
     * @return this;支持链式调用
     */
    public ValueConverters register(ValueConverter converter) {
        converters.add(converter);
        return this;
    }

    /**
     * 按目标类型找转换器
     *
     * @param targetType 实体字段 Java 类型
     *
     * @return 首个支持该类型的转换器
     */
    public Optional<ValueConverter> find(Class<?> targetType) {
        return converters.stream().filter(c -> c.supports(targetType)).findFirst();
    }

    /**
     * 按目标类型转换字符串值
     * <p>
     * 仅在传入值为字符串时触发转换;已是目标类型则原样返回。无匹配转换器时原样返回,由下游决定如何处理。
     *
     * @param rawValue   原始值;可为任意类型
     * @param targetType 实体字段 Java 类型
     *
     * @return 转换后的值;无匹配转换器时返回原值
     *
     * @throws IllegalArgumentException 转换失败
     */
    public Object convert(Object rawValue, Class<?> targetType) {
        if (rawValue == null || !(rawValue instanceof String) || targetType == null || targetType == String.class) {
            return rawValue;
        }
        return find(targetType)
                .map(converter -> converter.convert((String) rawValue, targetType))
                .orElse(rawValue);
    }
}