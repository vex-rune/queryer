# vex-queryer

轻量级 JPA 动态查询库，支持 predicate/order/page 三件套。

## 模块结构

```
vex-queryer/                      # 父模块
├── vex-queryer-model/            # 存储无关层：查询模型 + 通用执行器契约 + 检验/转换（不依赖 JPA）
│   └── src/main/java/com/vex/queryer/
│       ├── QueriesExecutor.java     # 通用查询执行器抽象基类（自动加载默认检验/转换）
│       ├── DataScopeFilter.java     # 数据域过滤器（查库/缓存/死值均可，自动 AND 进全部查询）
│       ├── QueryLimits.java        # 查询限制器（条件数/排序数/嵌套深度，默认嵌套 3 层，构造可覆盖）
│       ├── QueryValidationException.java # 查询参数校验异常（兜底类型，可按 IAE 捕获）
│       ├── QueryFieldValidationException.java # 字段维度异常（非法字段/路径穿透/排序字段非法）
│       ├── QueryValueValidationException.java # 值维度异常（值类型/格式非法/in 集合非法）
│       ├── model/
│       │   ├── QueriesCondition.java    # 单个比较条件
│       │   ├── QueriesCriteria.java     # 条件项（叶子/嵌套）
│       │   ├── QueriesOperatorEnum.java # 操作符枚举
│       │   ├── QueriesOrder.java        # 排序条件
│       │   ├── QueriesPage.java         # 分页参数
│       │   ├── QueriesPageRequest.java  # 统一查询请求体
│       │   └── QueriesPredicate.java    # 条件组合
│       ├── converter/              # 查询值转换器（时间/枚举/数值/布尔，可注册）
│       └── guard/                  # 查询检验器（字段规模/操作符契约/in 归一，可注册）
├── vex-queryer-example/          # 使用示例（Spring Boot + H2，mvn spring-boot:run 一键跑）
│   └── src/main/java/com/vex/queryer/example/
│       ├── demo/                   # 7 个角度示例（基础查询/转换器/检验器/数据域/限制器/异常/自定义执行器）
│       ├── custom/                 # 自定义转换器/检验器/数据域过滤器/内存执行器
│       └── domain/                 # 示例实体与仓储
└── vex-queryer-jpa/              # JPA 查询执行器（依赖 model）
    └── src/main/java/com/vex/queryer/
        └── JpaQueriesExecutor.java  # JPA 执行器（QueriesExecutor 的 JPA 实现）
```

## 快速开始

```xml
<dependency>
    <groupId>com.vex</groupId>
    <artifactId>vex-queryer-jpa</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 从 GitHub Packages 拉取

发布包托管在 GitHub Packages，需要在 `~/.m2/settings.xml` 中配置访问凭据：

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>ghp_xxx</password> <!-- PAT，至少需要 read:packages 权限 -->
    </server>
  </servers>
</settings>
```

同时在 `pom.xml` 中添加仓库：

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/vex-rune/queryer</url>
  </repository>
</repositories>
```

## 使用示例

```java
@Autowired
private EntityManager entityManager;

// 构建查询条件
QueriesPageRequest req = new QueriesPageRequest();
req.addAndCondition(QueriesCondition.of("name", QueriesOperatorEnum.like, "%测试%"));
req.setPage(new QueriesPage(0, 20));
req.setOrder(List.of(new QueriesOrder("createdAt", QueriesOrder.QueriesOrderEnum.DESC)));

// 执行查询
JpaQueriesExecutor<MyEntity> executor = JpaQueriesExecutor.of(MyEntity.class, entityManager);
List<MyEntity> list = executor.page(req);
long total = executor.count();
```

## 操作符

| 操作符 | 说明 |
|--------|------|
| eq | 等于 |
| ne | 不等于 |
| gt | 大于 |
| gte | 大于等于 |
| lt | 小于 |
| lte | 小于等于 |
| like | 模糊匹配 (SQL LIKE) |
| exp | 正则匹配 (PostgreSQL regexp_like) |
| in | IN 查询 |
| isNull | IS NULL |
| isNotNull | IS NOT NULL |
