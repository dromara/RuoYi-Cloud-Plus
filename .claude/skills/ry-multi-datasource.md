# RuoYi-Cloud-Plus Multi-Datasource Configuration

Dragonboat-Backend 基于 RuoYi-Cloud-Plus 框架的多数据源配置指南。

## 触发词

- "多数据源配置"
- "multi datasource"
- "多数据库配置"
- "读写分离"
- "数据源切换"
- "dynamic datasource"

## 功能概述

### 核心特性

基于 `dynamic-datasource` 框架实现多数据源功能：

- **数据源分组** - 支持多库、读写分离、一主多从、混合模式
- **异构数据库** - 支持 MySQL + Oracle + PostgreSQL 等混合使用
- **敏感配置加密** - 支持 ENC() 加密配置
- **懒加载** - 支持无数据源启动，按需加载
- **动态管理** - 运行时动态添加/移除数据源
- **Spel 参数解析** - 支持 spel、session、header 动态切换
- **嵌套切换** - 支持多层嵌套切换 (ServiceA → ServiceB → ServiceC)
- **分布式事务** - 基于 Seata 的分布式事务
- **本地多数据源事务** - 多数据源本地事务回滚

### 加载顺序

```
方法注解 > 类注解 > 默认数据源
```

## 数据源配置

### 1. YML 配置方式

**位置**: `application-common.yml`

```yaml
spring:
  datasource:
    dynamic:
      # 设置默认数据源
      primary: master
      # 严格模式，匹配不到数据源则报错
      strict: true
      datasource:
        # 主数据源
        master:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://localhost:3306/ry-vue3?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8
          username: root
          password: root

        # 从数据源
        slave:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://localhost:3306/ry-vue3-slave?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8
          username: root
          password: root

        # Oracle 数据源
        oracle:
          driver-class-name: oracle.jdbc.OracleDriver
          url: jdbc:oracle:thin:@localhost:1521:orcl
          username: system
          password: oracle

        # PostgreSQL 数据源
        postgresql:
          driver-class-name: org.postgresql.Driver
          url: jdbc:postgresql://localhost:5432/ry-vue3
          username: postgres
          password: postgres
```

### 2. 数据源分组配置

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        # 主库组
        master_1:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://localhost:3306/db1
          username: root
          password: root

        master_2:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://localhost:3306/db2
          username: root
          password: root

        # 从库组
        slave_1:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://localhost:3306/db1-slave
          username: root
          password: root

        slave_2:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://localhost:3306/db2-slave
          username: root
          password: root
```

### 3. 添加其他数据库依赖

**位置**: `ruoyi-common-mybatis/pom.xml`

```xml
<!-- Oracle JDBC -->
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc11</artifactId>
</dependency>

<!-- PostgreSQL JDBC -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- SQL Server JDBC -->
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
</dependency>

<!-- 达梦数据库 JDBC -->
<dependency>
    <groupId>com.dameng</groupId>
    <artifactId>DmJdbcDriver18</artifactId>
</dependency>
```

## 使用 @DS 注解切换数据源

### 1. 方法级别切换

```java
@Service
public class YourService {

    // 使用默认数据源
    public void defaultOperation() {
        // 使用 master 数据源
    }

    // 切换到 slave 数据源
    @DS("slave")
    public void slaveOperation() {
        // 使用 slave 数据源
    }

    // 切换到 oracle 数据源
    @DS("oracle")
    public void oracleOperation() {
        // 使用 oracle 数据源
    }
}
```

### 2. 类级别切换

```java
@DS("slave")
@Service
public class SlaveOperationService {

    // 类中所有方法都使用 slave 数据源
    public void operation1() {
        // 使用 slave
    }

    public void operation2() {
        // 使用 slave
    }

    // 方法注解优先级高于类注解
    @DS("master")
    public void masterOperation() {
        // 使用 master
    }
}
```

### 3. Mapper 层切换

```java
@Mapper
public interface YourMapper extends BaseMapperPlus<YourEntity, YourVo> {

    // 使用默认数据源
    List<YourEntity> selectList();

    // 切换到 slave 数据源
    @DS("slave")
    List<YourEntity> selectFromSlave();
}
```

### 4. 嵌套切换

```java
@Service
public class ServiceA {

    @DS("master")
    public void operationA() {
        // 使用 master 数据源
        serviceB.operationB();
    }
}

@Service
public class ServiceB {

    @DS("slave")
    public void operationB() {
        // 使用 slave 数据源
        serviceC.operationC();
    }
}

@Service
public class ServiceC {

    @DS("oracle")
    public void operationC() {
        // 使用 oracle 数据源
        // 切换链: master → slave → oracle
    }
}
```

## 事务管理

### 1. 单数据源事务

```java
@Service
public class YourService {

    // 使用 Spring 原生 @Transactional
    @Transactional(rollbackFor = Exception.class)
    public void singleTransaction() {
        // 单数据源事务
    }
}
```

### 2. 本地多数据源事务

**使用 `@DSTransactional` 注解**

```java
@Service
public class YourService {

    // 主数据源操作
    @DS("master")
    @DSTransactional
    public void multiDatasourceOperation() {
        // 使用 master 数据源
        masterService.insertData();
        slaveService.insertData();
        oracleService.insertData();

        // 任一环节异常，全部回滚
    }

    // 从数据源操作
    @DS("slave")
    public void slaveOperation() {
        // 使用 slave 数据源
    }

    // Oracle 数据源操作
    @DS("oracle")
    public void oracleOperation() {
        // 使用 oracle 数据源
    }
}
```

**注意事项**:
- `@DSTransactional` 会代理 `@DS` 注解切换后的数据源
- 任一环节发生异常，全局回滚
- 不能与原生 `@Transactional` 混用

### 3. 分布式事务（Seata）

**使用 `@GlobalTransactional` 注解**

```java
@Service
public class YourService {

    // 分布式事务
    @DS("master")
    @GlobalTransactional(rollbackFor = Exception.class)
    public void distributedTransaction() {
        // 跨服务、跨数据源事务
        remoteServiceA.updateData();
        remoteServiceB.updateData();
    }
}
```

## 动态切换数据源

### 1. Spel 表达式切换

```java
@Service
public class YourService {

    // 从参数中获取数据源名称
    @DS("#datasource")
    public void operation(String datasource) {
        // datasource 参数决定使用哪个数据源
    }

    // 从对象属性中获取
    @DS("#bo.datasource")
    public void operation(YourBo bo) {
        // bo.datasource 决定数据源
    }
}
```

### 2. Session/Header 切换

```java
@Service
public class YourService {

    // 从 Session 中获取
    @DS("@sessionDataSource.getKey()")
    public void operation() {
        // 从 Spring Bean 中获取数据源
    }

    // 从 Header 中获取
    @DS("@headerDataSource.getKey()")
    public void operation() {
        // 从请求头中获取数据源
    }
}
```

### 3. 编程式切换

```java
@Service
public class YourService {

    public void dynamicSwitch() {
        // 手动切换到 slave 数据源
        DynamicDataSourceContextHolder.push("slave");

        try {
            // 使用 slave 数据源执行操作
            yourService.slaveOperation();
        } finally {
            // 清除数据源，恢复默认
            DynamicDataSourceContextHolder.clear();
        }
    }
}
```

## 读写分离

### 1. 配置主从数据源

```yaml
spring:
  datasource:
    dynamic:
      primary: master
      datasource:
        master:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://master-host:3306/db
          username: root
          password: root

        slave1:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://slave1-host:3306/db
          username: root
          password: root

        slave2:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://slave2-host:3306/db
          username: root
          password: root
```

### 2. 使用读写分离

```java
@Service
public class YourService {

    // 写操作使用主库
    @DS("master")
    public void insert(YourEntity entity) {
        yourMapper.insert(entity);
    }

    // 读操作使用从库
    @DS("slave1")
    public YourEntity selectById(Long id) {
        return yourMapper.selectById(id);
    }
}
```

## 动态添加/移除数据源

### 1. 动态添加数据源

```java
@Service
public class DataSourceService {

    @Autowired
    private DynamicDataSourceProvider dynamicDataSourceProvider;

    public void addDataSource(String name, DataSourceProperty dataSourceProperty) {
        // 创建新数据源配置
        DataSourceProperty property = new DataSourceProperty();
        property.setDriverClassName("com.mysql.cj.jdbc.Driver");
        property.setUrl("jdbc:mysql://localhost:3306/new_db");
        property.setUsername("root");
        property.setPassword("root");

        // 添加数据源
        dynamicDataSourceProvider.addDataSource(name, property);
    }
}
```

### 2. 动态移除数据源

```java
@Service
public class DataSourceService {

    @Autowired
    private DynamicDataSourceProvider dynamicDataSourceProvider;

    public void removeDataSource(String name) {
        // 移除数据源
        dynamicDataSourceProvider.removeDataSource(name);
    }
}
```

## 常见场景

### 场景 1: 报表统计

```java
@Service
public class ReportService {

    /**
     * 从多个数据源收集数据生成报表
     */
    public Map<String, Object> generateReport() {
        Map<String, Object> report = new HashMap<>();

        // 从主库获取业务数据
        List<Order> orders = getOrdersFromMaster();
        report.put("orders", orders);

        // 从从库获取历史数据
        List<Order> historyOrders = getOrdersFromSlave();
        report.put("historyOrders", historyOrders);

        return report;
    }

    @DS("master")
    private List<Order> getOrdersFromMaster() {
        return orderMapper.selectList(null);
    }

    @DS("slave")
    private List<Order> getOrdersFromSlave() {
        return orderMapper.selectList(null);
    }
}
```

### 场景 2: 数据同步

```java
@Service
public class SyncService {

    /**
     * 从主库同步数据到从库
     */
    @DS("master")
    @DSTransactional
    public void syncData(Long id) {
        // 从主库查询数据
        YourEntity entity = masterMapper.selectById(id);

        // 写入从库
        slaveMapper.insert(entity);
    }
}
```

### 场景 3: 异构数据库操作

```java
@Service
public class CrossDatabaseService {

    /**
     * 跨异构数据库操作
     */
    @DS("master")
    @DSTransactional
    public void crossDatabaseOperation(Long id) {
        // MySQL 操作
        YourEntity entity = mysqlMapper.selectById(id);

        // 切换到 Oracle 操作
        oracleService.insertToOracle(entity);

        // 切换到 PostgreSQL 操作
        postgresqlService.insertToPostgresql(entity);
    }
}
```

## 注意事项

### 1. 禁止操作

- ❌ 禁止 `@DSTransactional` 与 `@Transactional` 混用
- ❌ 禁止在事务中动态切换数据源
- ❌ 禁止忘记清除手动切换的数据源

### 2. 推荐做法

- ✅ 明确指定数据源名称
- ✅ 使用 try-finally 清除手动切换
- ✅ 合理规划数据源数量（避免过多）
- ✅ 异构数据库注意 SQL 语法差异

### 3. 性能优化

- ✅ 使用连接池配置
- ✅ 合理设置数据源参数
- ✅ 监控数据源连接数
- ✅ 使用读写分离减轻主库压力

## 故障排除

### 问题 1: 数据源切换不生效

**原因**: 注解位置错误或优先级问题

**解决方案**:
```java
// ✅ 正确: 在 Service 或 Mapper 层使用
@Service
public class YourService {
    @DS("slave")
    public void operation() { }
}

// ❌ 错误: 在 Controller 层使用（无效）
@RestController
public class YourController {
    @DS("slave") // 无效
    public void operation() { }
}
```

### 问题 2: 事务回滚失败

**原因**: 事务注解使用错误

**解决方案**:
```java
// ✅ 正确: 使用 @DSTransactional
@DS("master")
@DSTransactional
public void operation() { }

// ❌ 错误: 使用 @Transactional
@DS("master")
@Transactional // 不支持多数据源
public void operation() { }
```

## 参考资源

- [dynamic-datasource 官方文档](https://www.kancloud.cn/tracy5546/dynamic-datasource/2264611)
- [RuoYi-Cloud-Plus 多数据源文档](https://plus-doc.dromara.org/ruoyi-cloud-plus/framework/extend/dynamic_datasource.html)
- [Seata 官方文档](https://seata.io/)

## 版本历史

- v1.0.0 (2026-01-17) - 初始版本，基于 RuoYi-Cloud-Plus 2.5.X
