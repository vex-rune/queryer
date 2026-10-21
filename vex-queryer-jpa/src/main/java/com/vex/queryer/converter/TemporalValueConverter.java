package com.vex.queryer.converter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 时间类型转换器 — Instant / LocalDate / LocalDateTime / OffsetDateTime。
 *
 * <p>接受 ISO-8601 字符串({@code 2026-10-21T17:23:40.991+08:00} / {@code 2026-10-21T09:23:40Z} /
 * {@code 2026-10-21})与毫秒时间戳;无时区的 {@code LocalDateTime} 按 UTC 补齐,保证与
 * {@code timestamptz} 列比较时语义确定。
 */
public class TemporalValueConverter implements ValueConverter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean supports(Class<?> targetType) {
        return targetType == Instant.class
                || targetType == LocalDate.class
                || targetType == LocalDateTime.class
                || targetType == OffsetDateTime.class;
    }

    @Override
    public Object convert(String rawValue, Class<?> targetType) {
        if (targetType == Instant.class) {
            return toInstant(rawValue);
        }
        if (targetType == LocalDate.class) {
            return toInstant(rawValue).atZone(ZoneId.of("UTC")).toLocalDate();
        }
        if (targetType == LocalDateTime.class) {
            return LocalDateTime.ofInstant(toInstant(rawValue), ZoneId.of("UTC"));
        }
        return toInstant(rawValue).atZone(ZoneId.of("UTC")).toOffsetDateTime();
    }

    /// 多种输入格式归一到 Instant;毫秒时间戳优先按纯数字识别
    private Instant toInstant(String raw) {
        String value = raw.trim();
        try {
            if (value.matches("-?\\d+")) {
                return Instant.ofEpochMilli(Long.parseLong(value));
            }
            if (value.contains("T")) {
                return Instant.parse(value);
            }
            // 无 'T' 的形式视为 UTC 挂钟时间(2026-10-21 / 2026-10-21 17:23:40)
            return parseClock(value).toInstant(ZoneOffset.UTC);
        } catch (Exception ex) {
            throw new IllegalArgumentException("时间值格式不合法: value=" + raw
                    + ", 期望 ISO-8601 字符串或毫秒时间戳", ex);
        }
    }

    /// 无时区分隔符的挂钟串 — 纯日期补零点后接秒
    private LocalDateTime parseClock(String value) {
        if (value.length() <= 10) {
            return LocalDate.parse(value, DATE).atStartOfDay();
        }
        return LocalDateTime.parse(value, CLOCK);
    }
}