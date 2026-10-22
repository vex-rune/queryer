package com.vex.queryer.example.demo;

import com.vex.queryer.example.domain.ArticleEntity;
import com.vex.queryer.example.domain.ArticleRepository;
import com.vex.queryer.example.domain.ArticleStatus;
import com.vex.queryer.example.domain.SkuCode;

import java.time.Instant;
import java.util.List;

/**
 * 示例数据 — 5 篇文章,覆盖多主体、多状态、值对象与时间列。
 *
 * <p>a1/a3/a4 属于 u-1001,a2/a5 属于 u-1002;SKU 存储为大写。
 */
public final class DemoData {

    public static final String SUBJECT_A = "u-1001";
    public static final String SUBJECT_B = "u-1002";

    private DemoData() {
    }

    public static void seed(ArticleRepository repository) {
        repository.deleteAll();
        repository.saveAll(List.of(
                article("a1", "动态查询入门", SUBJECT_A, ArticleStatus.PUBLISHED, "SKU-A", 30, "2026-10-05T08:00:00Z", "正文一"),
                article("a2", "条件组合实战", SUBJECT_B, ArticleStatus.PUBLISHED, "SKU-B", 80, "2026-10-10T08:00:00Z", "正文二"),
                article("a3", "分页排序", SUBJECT_A, ArticleStatus.DRAFT, "SKU-C", 10, "2026-10-15T08:00:00Z", "正文三"),
                article("a4", "数据域控制", SUBJECT_A, ArticleStatus.ARCHIVED, "SKU-D", 5, "2026-10-01T08:00:00Z", "正文四"),
                article("a5", "转换器与检验器", SUBJECT_B, ArticleStatus.DRAFT, "SKU-E", 60, "2026-11-01T08:00:00Z", "正文五")));
        repository.flush();
    }

    private static ArticleEntity article(String id, String title, String authorId, ArticleStatus status,
                                         String skuCode, int viewCount, String publishedAt, String content) {
        ArticleEntity a = new ArticleEntity();
        a.setId(id);
        a.setTitle(title);
        a.setAuthorId(authorId);
        a.setStatus(status);
        a.setSkuCode(new SkuCode(skuCode));
        a.setViewCount(viewCount);
        a.setPublishedAt(Instant.parse(publishedAt));
        a.setContent(content);
        return a;
    }
}
