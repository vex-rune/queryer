package com.vex.queryer.example.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 示例仓储 — 仅演示用;查询统一走 {@code JpaQueriesExecutor}。
 */
public interface ArticleRepository extends JpaRepository<ArticleEntity, String> {
}
