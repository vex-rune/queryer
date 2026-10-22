package com.vex.queryer.example.custom;

import com.vex.queryer.converter.ValueConverter;
import com.vex.queryer.example.domain.SkuCode;

/**
 * 自定义查询值转换器 — 把查询条件里的原始字符串归一为 {@code SkuCode}。
 *
 * <p>演示"业务自定义类型"的转换:内置转换器不认识 {@code SkuCode},
 * 注册本转换器后,{@code skuCode eq "sku-a"} 会先归一为 {@code SkuCode("SKU-A")} 再比较。
 */
public class SkuCodeValueConverter implements ValueConverter {

    @Override
    public boolean supports(Class<?> targetType) {
        return targetType == SkuCode.class;
    }

    @Override
    public Object convert(String rawValue, Class<?> targetType) {
        return new SkuCode(rawValue);
    }
}
