package com.vex.queryer;

import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesOperatorEnum;
import com.vex.queryer.model.QueriesPage;
import com.vex.queryer.model.QueriesPageRequest;
import com.vex.queryer.model.QueriesPredicate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 数据域过滤器钩子测试;验证基类注入/解析语义,不连 DB。
 */
class DataScopeFilterTest {

    @Test
    void noFilterInjectedReturnsNull() {
        assertNull(new ProbeExecutor().dataScopeConditions());
    }

    @Test
    void injectedFilterReturned() {
        List<QueriesCondition> scope = List.of(
                QueriesCondition.of("subjectId", QueriesOperatorEnum.eq, "s-1"));
        ProbeExecutor executor = new ProbeExecutor();
        executor.dataScope(() -> scope);

        assertSame(scope, executor.dataScopeConditions());
    }

    @Test
    void injectingNullClearsFilter() {
        ProbeExecutor executor = new ProbeExecutor();
        executor.dataScope(() -> List.of(QueriesCondition.of("subjectId", QueriesOperatorEnum.eq, "s-1")));
        executor.dataScope(null);

        assertNull(executor.dataScopeConditions());
    }

    /// 测试用执行器 — 仅验证基类数据域钩子;查询入口不实现
    private static final class ProbeExecutor extends QueriesExecutor<String> {
        @Override
        public List<String> page(QueriesPageRequest req) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> page(QueriesPage page) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> list() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long count() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long count(QueriesPredicate predicate) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean exists(QueriesPredicate predicate) {
            throw new UnsupportedOperationException();
        }
    }
}
