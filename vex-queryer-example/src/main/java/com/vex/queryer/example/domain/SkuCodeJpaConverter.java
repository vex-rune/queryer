package com.vex.queryer.example.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@code SkuCode} 与数据库列的互转 — 值对象以 VARCHAR 存储。
 */
@Converter(autoApply = true)
public class SkuCodeJpaConverter implements AttributeConverter<SkuCode, String> {

    @Override
    public String convertToDatabaseColumn(SkuCode attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public SkuCode convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new SkuCode(dbData);
    }
}
