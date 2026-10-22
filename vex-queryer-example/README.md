# vex-queryer-example

使用示例 + 测试模块 — 从 7 个角度演示查询库;Spring Boot + H2 演示(自包含),并承载必须由真实 PostgreSQL 支撑的集成测试。

## 运行示例(演示用 H2,无需外部数据库)

```bash
mvn -pl vex-queryer-example spring-boot:run
```

启动后按顺序打印全部示例输出(`DemoRunner` 编排)。

## 集成测试(必须有 pgsql)

`PostgresQueryIT`(22 例)必须由真实 PostgreSQL 支撑 — Testcontainers 自动拉起
`postgres:15-alpine`,验证 `regexp_like` / `timestamptz` 等 PG 方言行为,以及
数据域过滤 / 限制器 / 异常细分在真实 DB 上的表现。无 Docker 时用 `-DskipITs=true` 跳过。

```bash
mvn -pl vex-queryer-example verify           # 含 PG 集成测试(需 Docker)
mvn -pl vex-queryer-example -DskipITs=true verify
```

## 目录结构

```
src/main/java/com/vex/queryer/example/
├── ExampleApplication.java    # Spring Boot 入口
├── DemoRunner.java            # 依次执行 7 个角度示例
├── domain/                    # 示例实体 ArticleEntity / 枚举 / 值对象 SkuCode / 仓储
├── custom/                    # 自定义组件
│   ├── SkuCodeValueConverter.java     # 自定义查询值转换器
│   ├── SensitiveFieldGuard.java       # 自定义检验器（拦截敏感字段与操作符）
│   ├── SubjectScopeFilter.java        # 数据域过滤器（查库/缓存/死值均可）
│   ├── SubjectContext.java            # ThreadLocal 主体上下文（缓存场景）
│   └── InMemoryQueriesExecutor.java   # 自定义存储执行器（不连库，抽象基类模板）
└── demo/                      # 7 个角度示例
    ├── BasicQueryDemo.java    # ① 基础三件套 predicate/order/page/count/exists
    ├── ConverterDemo.java     # ② 值转换器（内置 + 自定义注册）
    ├── GuardDemo.java         # ③ 自定义检验器（写 SQL 前拦截）
    ├── DataScopeDemo.java     # ④ 数据域过滤（查库/缓存/死值，自动 AND）
    ├── LimitsDemo.java        # ⑤ 限制器（默认 3 层嵌套，构造可覆盖）
    ├── ExceptionDemo.java     # ⑥ 异常细分（字段异常 vs 值异常）
    └── CustomExecutorDemo.java # ⑦ 自定义存储执行器（面向 QueriesExecutor 抽象）

src/test/java/com/vex/queryer/
├── PostgresQueryIT.java       # PG 集成测试（22 例）
└── support/                   # PostgresTestContainer 容器基类 / 测试实体 DeviceEntity / 测试应用
```

## 各角度要点

| 角度 | 入口 | 展示内容 |
|------|------|----------|
| 基础查询 | `BasicQueryDemo` | 枚举按 `@JsonValue` 外部码、时间字符串归一、分页 0 起始、count 复用谓词 |
| 值转换器 | `ConverterDemo` | 内置枚举/时间免费;自定义类型经 `registerConverter` 归一后匹配 |
| 检验器 | `GuardDemo` | `registerGuard` 在写 SQL 前拦截敏感字段(字段异常)/禁止操作符(值异常) |
| 数据域 | `DataScopeDemo` | `dataScope` 自动 AND 进 page/count;主体来源固定值、ThreadLocal、查库 |
| 限制器 | `LimitsDemo` | 默认嵌套 3 层拒 4 层;构造 `QueryLimits` 放宽;排序数收紧被拒 |
| 异常 | `ExceptionDemo` | `QueryFieldValidationException` / `QueryValueValidationException` 分别映射 400 |
| 自定义执行器 | `CustomExecutorDemo` | 内存执行器复用基类检验/转换/数据域/限制;未来 Mongo 同构模板 |
