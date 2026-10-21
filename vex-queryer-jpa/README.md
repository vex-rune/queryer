# vex-queryer

轻量级 JPA 动态查询库，支持 predicate/order/page 三件套。

## 模块结构

```
vex-queryer/                      # 父模块
├── vex-queryer-model/            # 查询模型：predicate/order/page 三件套 POJO（不依赖 JPA）
│   └── src/main/java/com/vex/queryer/model/
│       ├── QueriesCondition.java    # 单个比较条件
│       ├── QueriesCriteria.java     # 条件项（叶子/嵌套）
│       ├── QueriesOperatorEnum.java # 操作符枚举
│       ├── QueriesOrder.java        # 排序条件
│       ├── QueriesPage.java         # 分页参数
│       ├── QueriesPageRequest.java  # 统一查询请求体
│       └── QueriesPredicate.java    # 条件组合
└── vex-queryer-jpa/              # JPA 查询执行器（依赖 model）
    └── src/main/java/com/vex/queryer/
        └── JpaQueriesExecutor.java  # JPA 执行器
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
