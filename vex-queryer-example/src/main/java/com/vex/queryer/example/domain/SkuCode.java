package com.vex.queryer.example.domain;

import java.util.Locale;

/**
 * 示例值对象 — SKU 编码;构造期归一为全大写。
 *
 * <p>作为"自定义查询值转换器"的演示目标类型:内置转换器不认识本类型,
 * 需注册 {@code SkuCodeValueConverter} 才能在查询时把原始字符串归一为 {@code SkuCode}。
 */
public record SkuCode(String value) {

    public SkuCode {
        value = value.trim().toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
