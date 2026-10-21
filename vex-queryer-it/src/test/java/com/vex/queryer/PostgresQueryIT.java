package com.vex.queryer;

import com.vex.queryer.model.QueriesCondition;
import com.vex.queryer.model.QueriesCriteria;
import com.vex.queryer.model.QueriesOperatorEnum;
import com.vex.queryer.model.QueriesOrder;
import com.vex.queryer.model.QueriesPage;
import com.vex.queryer.model.QueriesPageRequest;
import com.vex.queryer.model.QueriesPredicate;
import com.vex.queryer.support.DeviceEntity;
import com.vex.queryer.support.DeviceRepository;
import com.vex.queryer.support.DeviceRole;
import com.vex.queryer.support.DeviceStatus;
import com.vex.queryer.support.ItTestApplication;
import com.vex.queryer.support.PostgresTestContainer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL 集成测试 — 用真实 PG 验证 SQL 生成与执行。
 *
 * <p>核心价值:复现并锁死原始故障。JSON 反序列化后 {@code createdAt} 条件值退化为字符串,
 * 直接与 {@code timestamptz} 列比较会抛
 * {@code operator does not exist: timestamp with time zone >= character varying};
 * 值转换器正是为此而加,本测试确保它不再复发。
 */
@SpringBootTest(classes = ItTestApplication.class)
class PostgresQueryIT extends PostgresTestContainer {

    private static final Instant T1 = Instant.parse("2026-10-21T09:23:40Z");
    private static final Instant T2 = Instant.parse("2026-10-22T09:23:40Z");
    private static final Instant T3 = Instant.parse("2026-10-23T09:23:40Z");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DeviceRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.flush();
        repository.saveAll(List.of(
                device("d1", "gw-001", "网关一号", DeviceStatus.ONLINE, DeviceRole.GATEWAY, 10, true, T1, "备注1"),
                device("d2", "sn-002", "传感器二号", DeviceStatus.OFFLINE, DeviceRole.SENSOR, 20, false, T2, null),
                device("d3", "cm-003", "摄像头三", DeviceStatus.DRAFT, DeviceRole.CAMERA, 30, true, T3, "备注3")));
        repository.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("时间条件用字符串入参能正确落到 timestamptz 列 — 原始故障回归锁")
    void 时间条件字符串入参可执行() {
        // 这是原始报错的现场:createdAt 列是 timestamptz,请求体里却是 ISO 字符串
        QueriesPageRequest req = new QueriesPageRequest();
        req.addAndCondition(QueriesCondition.of("createdAt", QueriesOperatorEnum.gte, "2026-10-21T00:00:00Z"));

        List<DeviceEntity> rows = JpaQueriesExecutor.of(DeviceEntity.class, entityManager).page(req);

        assertEquals(3, rows.size(), "三行都晚于 2026-10-21T00:00:00Z");
    }

    @Test
    @DisplayName("时间上下界区间过滤")
    void 时间区间过滤() {
        QueriesPageRequest req = new QueriesPageRequest();
        req.addAndCondition(QueriesCondition.of("createdAt", QueriesOperatorEnum.gte, "2026-10-21T09:23:40Z"));
        req.addAndCondition(QueriesCondition.of("createdAt", QueriesOperatorEnum.lt, "2026-10-23T09:23:40Z"));

        List<DeviceEntity> rows = JpaQueriesExecutor.of(DeviceEntity.class, entityManager).page(req);

        assertEquals(2, rows.size(), "含边界,不含上界");
    }

    @Test
    @DisplayName("count 走时间条件 — 与 list 语义一致")
    void count走时间条件() {
        QueriesPageRequest req = new QueriesPageRequest();
        req.addAndCondition(QueriesCondition.of("createdAt", QueriesOperatorEnum.gte, "2026-10-22T00:00:00Z"));
        QueriesPredicate predicate = req.getPredicate();

        long total = JpaQueriesExecutor.of(DeviceEntity.class, entityManager).count(predicate);

        assertEquals(2, total);
    }

    @Test
    @DisplayName("枚举按 name 归一后可比较")
    void 枚举按名称归一() {
        QueriesPageRequest req = new QueriesPageRequest();
        req.addAndCondition(QueriesCondition.of("deviceStatus", QueriesOperatorEnum.eq, "ONLINE"));

        List<DeviceEntity> rows = JpaQueriesExecutor.of(DeviceEntity.class, entityManager).page(req);

        assertEquals(1, rows.size());
        assertEquals(DeviceStatus.ONLINE, rows.get(0).getDeviceStatus());
    }

    @Test
    @DisplayName("枚举按 @JsonValue 外部值归一")
    void 枚举按外部值归一() {
        QueriesPageRequest req = new QueriesPageRequest();
        req.addAndCondition(QueriesCondition.of("deviceRole", QueriesOperatorEnum.eq, "gw"));

        List<DeviceEntity> rows = JpaQueriesExecutor.of(DeviceEntity.class, entityManager).page(req);

        assertEquals(1, rows.size());
        assertEquals(DeviceRole.GATEWAY, rows.get(0).getDeviceRole());
    }

    @Test
    @DisplayName("in 集合元素按字段类型归一")
    void in集合元素归一() {
        QueriesPageRequest req = new QueriesPageRequest();
        req.addAndCondition(QueriesCondition.of("code", QueriesOperatorEnum.in,
                List.of("gw-001", "sn-002")));

        List<DeviceEntity> rows = JpaQueriesExecutor.of(DeviceEntity.class, entityManager).page(req);

        assertEquals(2, rows.size());
    }

    @Test
    @DisplayName("in 集合元素为时间字符串时逐个归一")
    void in集合时间元素归一() {
        QueriesPageRequest req = new QueriesPageRequest();
        req.addAndCondition(QueriesCondition.of("createdAt", QueriesOperatorEnum.in,
                List.of("2026-10-21T09:23:40Z", "2026-10-23T09:23:40Z")));

        List<DeviceEntity> rows = JpaQueriesExecutor.of(DeviceEntity.class, entityManager).page(req);

        assertEquals(2, rows.size());
    }

    @Test
    @DisplayName("数值与布尔归一后可比较")
    void 数值与布尔归一() {
        // 夹具: (d1,weight=10,启用) (d2,weight=20,停用) (d3,weight=30,启用)
        QueriesPageRequest req = new QueriesPageRequest();
        req.addAndCondition(QueriesCondition.of("weight", QueriesOperatorEnum.gt, "15"));
        req.addAndCondition(QueriesCondition.of("enabled", QueriesOperatorEnum.eq, "true"));

        List<DeviceEntity> rows = JpaQueriesExecutor.of(DeviceEntity.class, entityManager).page(req);

        assertEquals(1, rows.size(), "weight>15 的有 d2(停用) 与 d3(启用),启用条件下只剩 d3");
        assertEquals("d3", rows.get(0).getId());
    }

    @Test
    @DisplayName("like 模糊匹配")
    void like模糊匹配() {
        QueriesPageRequest req = new QueriesPageRequest();
        req.addAndCondition(QueriesCondition.of("name", QueriesOperatorEnum.like, "%号%"));

        List<DeviceEntity> rows = JpaQueriesExecutor.of(DeviceEntity.class, entityManager).page(req);

        assertEquals(2, rows.size());
    }

    @Test
    @DisplayName("exp 正则匹配走 PG regexp_like")
    void exp正则匹配() {
        QueriesPageRequest req = new QueriesPageRequest();
        req.addAndCondition(QueriesCondition.of("code", QueriesOperatorEnum.exp, "^(gw|sn)-"));

        List<DeviceEntity> rows = JpaQueriesExecutor.of(DeviceEntity.class, entityManager).page(req);

        assertEquals(2, rows.size());
    }

    @Test
    @DisplayName("isNull / isNotNull")
    void 空值判断() {
        QueriesPageRequest req = new QueriesPageRequest();
        req.addAndCondition(QueriesCondition.of("remark", QueriesOperatorEnum.isNull, null));
        assertEquals(1, JpaQueriesExecutor.of(DeviceEntity.class, entityManager).page(req).size());

        QueriesPageRequest req2 = new QueriesPageRequest();
        req2.addAndCondition(QueriesCondition.of("remark", QueriesOperatorEnum.isNotNull, null));
        assertEquals(2, JpaQueriesExecutor.of(DeviceEntity.class, entityManager).page(req2).size());
    }

    @Test
    @DisplayName("AND / OR 组合与嵌套")
    void 条件组合与嵌套() {
        // 外层 AND: ONLINE 且 (weight<25 或 weight>25)
        // 括号单元用「内层 predicate 只含 or」表达 —— 同一层级的 and/or 会被展平合并,
        // 无法直接作为括号单元,故需多包一层。
        QueriesPredicate innerOr = new QueriesPredicate();
        innerOr.setOr(List.of(
                QueriesCriteria.of(QueriesCondition.of("weight", QueriesOperatorEnum.lt, "25")),
                QueriesCriteria.of(QueriesCondition.of("weight", QueriesOperatorEnum.gt, "25"))));
        QueriesCriteria parenthesized = new QueriesCriteria();
        parenthesized.setPredicate(innerOr);

        QueriesPredicate outer = new QueriesPredicate();
        outer.setAnd(List.of(
                QueriesCriteria.of(QueriesCondition.of("deviceStatus", QueriesOperatorEnum.eq, "ONLINE")),
                parenthesized));

        QueriesPageRequest req = new QueriesPageRequest();
        req.setPredicate(outer);

        List<DeviceEntity> rows = JpaQueriesExecutor.of(DeviceEntity.class, entityManager).page(req);

        assertEquals(1, rows.size(), "ONLINE 仅 d1(weight=10),落在 <25 分支");
        assertEquals("d1", rows.get(0).getId());
    }

    @Test
    @DisplayName("排序与分页")
    void 排序与分页() {
        QueriesPageRequest req = new QueriesPageRequest();
        req.setOrder(List.of(new QueriesOrder("weight", QueriesOrder.QueriesOrderEnum.DESC)));
        req.setPage(new QueriesPage(0, 2));

        List<DeviceEntity> rows = JpaQueriesExecutor.of(DeviceEntity.class, entityManager).page(req);

        assertEquals(2, rows.size());
        assertEquals(30, rows.get(0).getWeight(), "按权重倒序,首行应为最大");
    }

    @Test
    @DisplayName("非法字段在写 SQL 前被拒绝")
    void 非法字段被拒绝() {
        QueriesPageRequest req = new QueriesPageRequest();
        req.addAndCondition(QueriesCondition.of("noSuchColumn", QueriesOperatorEnum.eq, "x"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> JpaQueriesExecutor.of(DeviceEntity.class, entityManager).page(req));
        assertTrue(ex.getMessage().contains("noSuchColumn"), ex.getMessage());
    }

    @Test
    @DisplayName("字段名路径穿透被检验器拒绝")
    void 字段路径穿透被拒绝() {
        QueriesPageRequest req = new QueriesPageRequest();
        req.addAndCondition(QueriesCondition.of("owner.secret", QueriesOperatorEnum.eq, "x"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> JpaQueriesExecutor.of(DeviceEntity.class, entityManager).page(req));
        assertTrue(ex.getMessage().contains("owner.secret"), ex.getMessage());
    }

    @Test
    @DisplayName("值格式非法时给出可定位的错误而非 SQL 异常")
    void 值格式非法给出可定位错误() {
        QueriesPageRequest req = new QueriesPageRequest();
        req.addAndCondition(QueriesCondition.of("createdAt", QueriesOperatorEnum.gte, "不是时间"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> JpaQueriesExecutor.of(DeviceEntity.class, entityManager).page(req));
        assertTrue(ex.getMessage().contains("不是时间"), ex.getMessage());
    }

    private DeviceEntity device(String id, String code, String name, DeviceStatus status,
                                DeviceRole role, int weight, boolean enabled, Instant createdAt, String remark) {
        DeviceEntity d = new DeviceEntity();
        d.setId(id);
        d.setCode(code);
        d.setName(name);
        d.setDeviceStatus(status);
        d.setDeviceRole(role);
        d.setWeight(weight);
        d.setEnabled(enabled);
        d.setCreatedAt(createdAt);
        d.setRemark(remark);
        return d;
    }
}