package com.vex.queryer.converter;

/**
 * 查询值转换器 — 把 JSON 反序列化残留的字符串转成实体字段的真实 Java 类型。
 *
 * <p>{@code QueriesCondition.value} 声明为 {@code Object},Jackson 反序列化时无法识别
 * {@code Instant} / 枚举等类型,只会保留字符串;直接拿字符串与 {@code timestamptz} 列比较会触发
 * SQL 语法错({@code operator does not exist: timestamp with time zone >= character varying})。
 *
 * <p>内置转换器覆盖时间 / 枚举 / 数值 / 布尔;业务自定义类型实现本接口后
 * {@link ValueConverters#register} 即可生效。
 *
 * <p>实现约定:
 * <ul>
 *     <li>{@link #supports} 只认目标类型,不认具体值 —— 避免歧义</li>
 *     <li>{@link #convert} 失败必须抛异常,不允许静默返回原值</li>
 *     <li>实现必须无状态且线程安全(单例注册,跨线程复用)</li>
 * </ul>
 */
public interface ValueConverter {

    /**
     * 是否支持该目标类型
     *
     * @param targetType 实体字段的 Java 类型
     *
     * @return true=本转换器能处理该类型
     */
    boolean supports(Class<?> targetType);

    /**
     * 转换单个值
     *
     * @param rawValue   原始值;保证非 null 且非空白
     * @param targetType 实体字段的 Java 类型
     *
     * @return 目标类型的值
     *
     * @throws IllegalArgumentException 值格式与目标类型不匹配
     */
    Object convert(String rawValue, Class<?> targetType);
}