package com.vex.queryer.example.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 示例状态枚举 — {@code @JsonValue} 提供对外码,演示内置枚举转换器按 name / 外部码双向归一。
 */
public enum ArticleStatus {
    DRAFT("0"),
    PUBLISHED("1"),
    ARCHIVED("2");

    private final String code;

    ArticleStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }
}
