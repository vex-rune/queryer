package com.vex.queryer;

import com.vex.queryer.model.QueriesPage;
import com.vex.queryer.model.QueriesPageRequest;
import com.vex.queryer.model.QueriesPredicate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 查询限制器测试 — 默认值、构造覆盖与参数校验。
 */
class QueryLimitsTest {

    @Test
    void defaultLimits() {
        assertEquals(200, QueryLimits.DEFAULT.maxConditions());
        assertEquals(10, QueryLimits.DEFAULT.maxOrders());
        assertEquals(3, QueryLimits.DEFAULT.maxDepth());
    }

    @Test
    void constructorOverridesLimits() {
        QueryLimits limits = new QueryLimits(100, 5, 2);

        assertEquals(100, limits.maxConditions());
        assertEquals(5, limits.maxOrders());
        assertEquals(2, limits.maxDepth());
    }

    @Test
    void nonPositiveLimitRejected() {
        assertThrows(IllegalArgumentException.class, () -> new QueryLimits(0, 10, 3));
        assertThrows(IllegalArgumentException.class, () -> new QueryLimits(200, 0, 3));
        assertThrows(IllegalArgumentException.class, () -> new QueryLimits(200, 10, 0));
    }

    @Test
    void baseClassAcceptsInjectedLimits() {
        ProbeExecutor executor = new ProbeExecutor(new QueryLimits(50, 5, 1));

        assertEquals(50, executor.limits().maxConditions());
        assertEquals(5, executor.limits().maxOrders());
        assertEquals(1, executor.limits().maxDepth());
    }

    /// 测试用执行器 — 仅验证基类限制器注入;查询入口不实现
    private static final class ProbeExecutor extends QueriesExecutor<String> {

        ProbeExecutor(QueryLimits limits) {
            super(limits);
        }

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
