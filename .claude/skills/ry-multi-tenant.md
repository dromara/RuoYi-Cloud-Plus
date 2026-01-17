# RuoYi-Cloud-Plus Multi-Tenant Development

Dragonboat-Backend 基于 RuoYi-Cloud-Plus 框架的多租户功能开发指南。

## 触发词

- "多租户开发"
- "租户配置"
- "tenant development"
- "租户隔离"
- "SaaS开发"
- "multi-tenant"

## 什么是多租户系统

### 核心概念

**多租户系统**是为了解决一套软件部署多份问题，使用共同的服务器为多家独立的公司服务。

- **租户与租户之间数据完全隔离** - 属于京东和淘宝的关系
- **平台级别的隔离** - 租户和租户之间完全没关系
- **SaaS系统** - 卖软件的，客户与客户之间毫无关系

### 常见误区

| 概念 | 是否多租户 | 说明 |
|------|----------|------|
| 平台下有多商户 | ❌ 否 | 这是业务概念，应该使用数据权限结合业务处理 |
| 大小租户 | ❌ 否 | 不符合多租户定义 |
| 上下级租户 | ❌ 否 | 这是业务层级关系 |
| SaaS多企业 | ✅ 是 | 每个企业是独立租户 |

> ⚠️ **重要警告**: 搞清楚再使用多租户功能，不然后果自负！

## 多租户配置

### 1. 基本配置

**位置**: `application-common.yml`

```yaml
# 多租户配置
tenant:
  # 是否开启多租户功能（默认已开启）
  enable: true

  # 不需要过滤租户的表
  excludes:
    - sys_user
    - sys_dept
    - sys_role
    - sys_menu
    - sys_tenant
    - sys_tenant_package
    - # 添加其他非租户表
```

### 2. 数据库表设计

**租户表必备字段**:
```sql
CREATE TABLE `your_business_table` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) COMMENT='业务表';
```

**Entity 配置**:
```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("your_business_table")
public class YourBusiness extends TenantEntity {
    // TenantEntity 已包含 tenant_id
    // 其他业务字段...
}
```

**非租户表**:
```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_global_config")
public class SysGlobalConfig extends BaseEntity {
    // BaseEntity 不包含 tenant_id
    // 全局配置字段...
}
```

### 3. 关键注意事项

> ⚠️ **如果已经基于租户模式启动了程序，关闭租户必须删除 MySQL 与 Redis 内的相关数据重新导入 SQL**

## 租户管理操作

### 1. 租户套餐管理

**流程**: 先新增套餐 → 再新增租户

```java
// 套餐包含的内容：
// 1. 租户可使用的菜单列表
// 2. 租户的功能权限范围
// 3. 租户的资源配额
```

**步骤**:
1. 登录超级管理员账号
2. 进入"租户管理" → "租户套餐管理"
3. 新增套餐，选择菜单权限
4. 配置套餐参数（用户数量、过期时间等）

### 2. 租户新增

**步骤**:
1. 进入"租户管理" → "租户列表"
2. 点击"新增"按钮
3. 填写租户信息
4. 选择租户套餐（重要：选择后无法修改）
5. 提交保存

**租户信息包含**:
- 租户编号（自动生成）
- 租户名称
- 联系人信息
- 租户套餐
- 域名绑定（可选）
- 过期时间
- 用户数量限制

### 3. 域名配置

**作用**: 绑定域名后，该域名只能选择对应的租户登录

```java
// 示例：
// company-a.example.com → 租户A
// company-b.example.com → 租户B
```

**注意**: 没有配置域名时，登录界面可以选择所有租户

### 4. 切换租户

**权限**: 仅超级管理员可操作

**说明**: 管理员切换租户不是切换用户，切换的只是数据，管理员拥有所有权限。

## 忽略租户过滤

### 1. Mapper 层忽略

**场景**: 需要查询所有租户的数据（如系统管理功能）

```java
@InterceptorIgnore(tenantLine = "true", dataPermission = "false")
List<YourEntity> selectAllTenantsData();
```

> ⚠️ **重要**: 使用此注解时，`dataPermission = "false"` 必须添加，否则会导致数据权限失效

### 2. 业务层忽略（推荐）

**场景**: 临时需要忽略租户过滤

```java
// 无返回值
TenantHelper.ignore(() -> {
    // 这里的代码不会过滤租户
    List<YourEntity> list = yourService.list();
});

// 有返回值
List<YourEntity> result = TenantHelper.ignore(() -> {
    return yourService.list();
});
```

**使用场景**:
- 系统初始化
- 跨租户数据同步
- 系统管理功能

### 3. 动态切换租户

> ⚠️ **仅适用于特殊需求业务，禁止乱用后果自负！**

**场景**:
- 创建租户时，对该租户操作一些数据
- 需要去其他租户查一些数据

```java
// 无返回值
TenantHelper.dynamic(tenantId, () -> {
    // 这里的代码会使用指定的租户ID
    yourService.save(entity);
});

// 有返回值
YourEntity result = TenantHelper.dynamic(tenantId, () -> {
    return yourService.getById(id);
});
```

## 多租户开发最佳实践

### 1. 表设计检查清单

- [ ] 租户表包含 `tenant_id` 字段
- [ ] Entity 继承 `TenantEntity`
- [ ] 非租户表配置到 `excludes`
- [ ] 非租户表 Entity 继承 `BaseEntity`

### 2. 代码开发检查清单

- [ ] 避免手动拼接 SQL（会导致租户过滤失效）
- [ ] 使用 MyBatis-Plus 方法（自动过滤租户）
- [ ] 特殊场景使用 `TenantHelper` 工具类
- [ ] Mapper 忽略租户时同时处理数据权限

### 3. 常见问题

#### 问题 1: 数据泄露

**现象**: 能看到其他租户的数据

**原因**:
1. 表没有 `tenant_id` 字段
2. Entity 没有继承 `TenantEntity`
3. 表没有配置到 `excludes`

**解决**: 检查表设计和配置

#### 问题 2: 数据权限失效

**现象**: 使用 `@InterceptorIgnore` 后数据权限不生效

**原因**: 注解参数不完整

**解决**:
```java
// ✅ 正确
@InterceptorIgnore(tenantLine = "true", dataPermission = "false")

// ❌ 错误
@InterceptorIgnore(tenantLine = "true")
```

#### 问题 3: 切换租户后数据不对

**现象**: 使用 `TenantHelper.dynamic()` 后数据不对

**原因**: 动态切换后没有切回原租户

**解决**: 使用返回值形式，确保作用域正确

## 租户测试要点

### 1. 隔离性测试

```java
// 测试：租户A不能看到租户B的数据
@Test
void testTenantIsolation() {
    // 租户A登录
    LoginHelper.login(tenantAUser);

    // 创建数据
    YourEntity entity = new YourEntity();
    yourService.save(entity);

    // 租户B登录
    LoginHelper.login(tenantBUser);

    // 查不到租户A的数据
    YourEntity result = yourService.getById(entity.getId());
    assertNull(result);
}
```

### 2. 忽略租户测试

```java
// 测试：能查询所有租户数据
@Test
void testIgnoreTenant() {
    List<YourEntity> allData = TenantHelper.ignore(() -> {
        return yourService.list();
    });

    // 包含所有租户的数据
    assertTrue(allData.size() > 0);
}
```

### 3. 动态切换测试

```java
// 测试：动态切换租户
@Test
void testDynamicTenant() {
    String tenantId = "000001";

    YourEntity entity = TenantHelper.dynamic(tenantId, () -> {
        return yourService.getById(1L);
    });

    // 返回的是指定租户的数据
    assertEquals(tenantId, entity.getTenantId());
}
```

## 多租户相关 API

### TenantHelper 工具类

```java
// 获取当前租户ID
String tenantId = TenantHelper.getTenantId();

// 动态指定租户ID
TenantHelper.dynamic(tenantId, () -> {
    // 业务代码
});

// 忽略租户
TenantHelper.ignore(() -> {
    // 业务代码
});

// 判断是否为忽略租户模式
boolean isIgnore = TenantHelper.isIgnore();
```

### TenantEntity 租户实体

```java
@Data
public class TenantEntity extends BaseEntity {
    /**
     * 租户编号
     */
    private String tenantId;
}
```

## 与其他功能的集成

### 1. 多租户 + 数据权限

**注意**: 同时使用时要注意注解配置

```java
// Mapper 方法
@InterceptorIgnore(tenantLine = "true", dataPermission = "false")
// tenantLine = "true" 忽略租户
// dataPermission = "false" 开启数据权限
```

### 2. 多租户 + 多数据源

**注意**: 每个数据源都需要配置租户插件

```java
// 多数据源配置会自动继承主数据源的租户配置
// 无需额外处理
```

### 3. 多租户 + 缓存

**注意**: 缓存 Key 会自动包含租户ID

```java
// 使用 @Cacheable 注解
@Cacheable(value = "user", key = "#userId")
public User getById(Long userId) {
    // Key 自动包含租户ID: user:tenantId:userId
}
```

## 开发规范

### 1. 禁止操作

- ❌ 禁止手动拼接 SQL 绕过租户过滤
- ❌ 禁止在非必要时使用 `TenantHelper.ignore()`
- ❌ 禁止滥用 `TenantHelper.dynamic()`
- ❌ 禁止忘记在 `@InterceptorIgnore` 中设置 `dataPermission`

### 2. 推荐做法

- ✅ 使用 MyBatis-Plus 方法
- ✅ Entity 继承 `TenantEntity`
- ✅ 特殊场景使用 `TenantHelper` 工具类
- ✅ 完善的租户隔离测试

### 3. 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 租户ID | `tenant_id` | 数据库字段 |
| 租户编号 | `000000` (默认租户) | 6位数字 |
| 租户表名 | `sys_tenant` | 租户管理表 |

## 故障排除

### 关闭多租户模式

> ⚠️ **严重警告**: 如果已经基于租户模式启动过程序，关闭租户必须：

1. 删除 MySQL 中所有 `tenant_id` 相关数据
2. 清空 Redis 缓存
3. 重新导入 SQL
4. 修改配置文件
5. 重启所有服务

### 调试租户问题

```java
// 查看当前租户ID
String currentTenantId = TenantHelper.getTenantId();
log.info("当前租户ID: {}", currentTenantId);

// 查看是否忽略租户
boolean isIgnore = TenantHelper.isIgnore();
log.info("是否忽略租户: {}", isIgnore);
```

## 参考资源

- [MyBatis-Plus 多租户插件](https://baomidou.com/pages/aef2f2/)
- [RuoYi-Cloud-Plus 多租户文档](https://plus-doc.dromara.org/ruoyi-cloud-plus/framework/basic/tenant.html)

## 版本历史

- v1.0.0 (2026-01-17) - 初始版本，基于 RuoYi-Cloud-Plus 2.X
