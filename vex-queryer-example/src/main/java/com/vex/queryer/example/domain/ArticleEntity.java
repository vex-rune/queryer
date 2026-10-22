package com.vex.queryer.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 示例实体 — 覆盖字符串/枚举/值对象/数值/时间(timestamptz)/敏感字段等多类列,
 * 用于从多个角度演示查询执行器。
 */
@Data
@Entity
@NoArgsConstructor
@Table(name = "example_article")
public class ArticleEntity {

    /** 主键;雪花风格字符串 id */
    @Id
    @Column(name = "id", length = 64)
    private String id;

    /** 标题;用于 like / eq 验证 */
    @Column(name = "title", length = 128)
    private String title;

    /** 作者/主体 id;用于数据域过滤演示 */
    @Column(name = "author_id", length = 64)
    private String authorId;

    /** 状态枚举;演示内置枚举转换器(@JsonValue / name) */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private ArticleStatus status;

    /** SKU 值对象;演示自定义查询值转换器 */
    @Column(name = "sku_code", length = 64)
    private SkuCode skuCode;

    /** 浏览量;用于数值比较与排序 */
    @Column(name = "view_count")
    private Integer viewCount;

    /** 发布时间(timestamptz);演示时间字符串归一 */
    @Column(name = "published_at")
    private Instant publishedAt;

    /** 正文;演示"敏感字段"检验器拦截 */
    @Column(name = "content", length = 1024)
    private String content;
}
