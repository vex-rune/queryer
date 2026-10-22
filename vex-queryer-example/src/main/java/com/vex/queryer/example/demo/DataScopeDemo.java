package com.vex.queryer.example.demo;

import com.vex.queryer.JpaQueriesExecutor;
import com.vex.queryer.example.custom.SubjectContext;
import com.vex.queryer.example.custom.SubjectScopeFilter;
import com.vex.queryer.example.domain.ArticleEntity;
import com.vex.queryer.example.domain.ArticleRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * 角度4 — 数据域过滤(自动加入 AND;查库 / 查缓存 / 固定死值,由过滤器决定)。
 *
 * <p>三种主体来源:固定死值、ThreadLocal 缓存、实时查库;取不到主体则本次不过滤。
 * count 同样受数据域约束。
 */
public class DataScopeDemo {

    public static void run(ArticleRepository repository, EntityManager entityManager) {
        System.out.println("\n========== 角度4: 数据域过滤(查库/缓存/死值,由过滤器决定) ==========");
        DemoData.seed(repository);

        // ① 固定死值:只看 u-1001 的数据
        long fixed = JpaQueriesExecutor.of(ArticleEntity.class, entityManager)
                .dataScope(new SubjectScopeFilter(() -> DemoData.SUBJECT_A))
                .count();
        System.out.println("固定死值 subject=" + DemoData.SUBJECT_A + " → count=" + fixed);

        // ② 查缓存(ThreadLocal 模拟每请求上下文)
        SubjectContext.set(DemoData.SUBJECT_B);
        long cached = JpaQueriesExecutor.of(ArticleEntity.class, entityManager)
                .dataScope(new SubjectScopeFilter(SubjectContext::get))
                .count();
        System.out.println("ThreadLocal subject=" + DemoData.SUBJECT_B + " → count=" + cached);
        SubjectContext.clear();

        // ③ 查库:按"浏览量最高文章的作者"决定可见域
        long db = JpaQueriesExecutor.of(ArticleEntity.class, entityManager)
                .dataScope(new SubjectScopeFilter(() -> repository
                        .findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "viewCount")))
                        .stream().findFirst().map(ArticleEntity::getAuthorId).orElse(null)))
                .count();
        System.out.println("查库解析 subject(最高浏览文章作者) → count=" + db);
    }
}
