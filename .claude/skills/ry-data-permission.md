# RuoYi-Cloud-Plus Data Permission Configuration

Dragonboat-Backend 基于 RuoYi-Cloud-Plus 框架的数据权限配置指南。

## 触发词

- "数据权限配置"
- "data permission"
- "数据权限模板"
- "部门权限"
- "自定义数据权限"
- "data scope"

## 数据权限概述

### 核心功能

数据权限系统用于控制用户能访问哪些数据，基于以下机制:

1. **自动 SQL 注入** - 自动在 SQL 中添加过滤条件
2. **查询/更新/删除限制** - 全面的数据操作控制
3. **自定义字段过滤** - 灵活的数据权限规则
4. **Spel 表达式支持** - 动态 Bean 处理
5. **与菜单权限联合** - 2.2.X+ 新功能

### 数据权限体系

```
用户 → 多角色 → 角色 → 单数据权限
```

**示例场景**:
- 用户A 拥有两个角色
- 角色A（部门经理）: 可查看本部门及以下部门的数据
- 角色B（兼职开发）: 可查看仅自己的数据

## 相关类与注解

| 类/注解 | 说明 | 功能 |
|---------|------|------|
| `DataScopeType` | 数据权限模板定义 | 定义数据权限 SQL 模板 |
| `@DataPermission` | 数据权限组注解 | 标注开启数据权限（默认过滤部门权限） |
| `@DataColumn` | 数据权限字段注解 | 替换模板中的变量 |
| `PlusDataPermissionInterceptor` | SQL 拦截器 | 检查 `@DataPermission` 注解 |
| `PlusDataPermissionHandler` | 数据权限处理器 | 添加数据权限过滤条件 |
| `DataPermissionHelper` | 数据权限助手 | 操作数据权限上下文 |
| `SysDataScopeService` | 自定义 Bean 处理 | 自定义扩展 |

## 基础使用

### 1. 预定义数据权限类型

框架内置的数据权限类型:

| 类型 | Code | 说明 |
|------|------|------|
| 全部数据 | `1` | 查看所有数据 |
| 自定义数据 | `2` | 自定义 SQL 模板 |
| 本部门数据 | `3` | 仅本部门 |
| 本部门及以下 | `4` | 本部门及子部门 |
| 仅本人 | `5` | 仅创建人 |

### 2. 在 Mapper 中使用数据权限

> ⚠️ **重要**: 数据权限注解只能在 **Mapper 层**使用

```java
@Mapper
public interface YourMapper extends BaseMapperPlus<YourEntity, YourVo> {

    /**
     * 查询列表（带数据权限）
     * dept_id: 部门过滤字段
     * user_id: 创建人过滤字段
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userId", value = "user_id")
    })
    List<YourEntity> selectList(YourBo bo);
}
```

**字段说明**:
- `key` - 对应 SQL 模板中的变量（如 `#{#deptName}`）
- `value` - 数据库中的实际字段名

### 3. 为 MyBatis-Plus 原生方法添加数据权限

**从 2.3.0 版本开始**，无需重写底层方法:

```java
@Mapper
public interface YourMapper extends BaseMapperPlus<YourEntity, YourVo> {

    /**
     * 分页查询（带数据权限）
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userId", value = "user_id")
    })
    default Page<YourVo> selectPageByPermission(Page<YourVo> page, YourBo bo) {
        // 直接调用 MyBatis-Plus 的方法
        return this.selectPage(page, buildLambdaQueryWrapper(bo));
    }

    /**
     * 列表查询（带数据权限）
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userId", value = "user_id")
    })
    default List<YourVo> selectListByPermission(YourBo bo) {
        // 直接调用 MyBatis-Plus 的方法
        return this.selectList(buildLambdaQueryWrapper(bo));
    }
}
```

### 4. 类级别标注（2.2.X+）

**作用域规则**: 方法 > 类

```java
@DataPermission({
    @DataColumn(key = "deptName", value = "dept_id"),
    @DataColumn(key = "userId", value = "user_id")
})
@Mapper
public interface YourMapper extends BaseMapperPlus<YourEntity, YourVo> {
    // 类中所有方法（包括父类方法）都会进行数据权限过滤

    List<YourEntity> selectList(YourBo bo);

    YourEntity selectById(Long id);

    // 可以用方法注解覆盖类注解
    @DataPermission({
        @DataColumn(key = "userId", value = "create_by")
    })
    List<YourEntity> selectMyData();
}
```

## 自定义数据权限模板

### 1. 添加自定义模板到角色管理

**位置**: 系统管理 → 角色管理 → 数据权限下拉框

```java
// 在 DataScopeType 中添加模板
public @interface DataScopeType {
    // 预定义模板
    String DEPT_CUSTOM = "deptCustom";

    // 自定义模板
    String PROJECT_CUSTOM = "projectCustom"; // 新增
}
```

### 2. 定义 SQL 模板

```java
public @interface DataScopeType {
    // ...

    /**
     * 项目自定义数据权限
     *
     * #{#projectName} - 项目名称变量
     * #{@sdss} - 自定义 Bean 调用
     * #{#deptName} - 部门名称变量（默认）
     */
    String PROJECT_CUSTOM =
        "project_id IN (SELECT project_id FROM sys_user_project WHERE user_id = #{#userId}) " +
        "OR " +
        "#{#projectName} IN (SELECT project_name FROM sys_user_project WHERE user_id = #{#userId})";

    // 兜底 SQL: 当角色与注解无对应时
    String ELSE_SQL = "1 = 0";
}
```

**模板变量说明**:
- `#{#variableName}` - Spel 表达式变量
- `#{@beanName}` - 调用 Spring Bean 方法
- `elseSql` - 兜底处理，防止查看不该看的数据

### 3. 在角色管理配置

1. 进入系统管理 → 角色管理
2. 编辑角色
3. 数据权限选择 "项目自定义"
4. 配置自定义变量值

### 4. 使用自定义权限

```java
@Mapper
public interface ProjectMapper extends BaseMapperPlus<Project, ProjectVo> {

    @DataPermission({
        @DataColumn(key = "projectName", value = "project_id"),
        @DataColumn(key = "userId", value = "create_by")
    })
    List<Project> selectList(ProjectBo bo);
}
```

## 忽略数据权限

### 1. Mapper 层忽略

**场景**: 需要查询所有数据（如系统管理、统计分析）

```java
@Mapper
public interface YourMapper extends BaseMapperPlus<YourEntity, YourVo> {

    @InterceptorIgnore(dataPermission = "true")
    List<YourEntity> selectAllWithoutPermission();
}
```

### 2. 业务层忽略（推荐）

**场景**: 临时需要忽略数据权限

```java
// 无返回值
DataPermissionHelper.ignore(() -> {
    // 这里的代码不会进行数据权限过滤
    List<YourEntity> list = yourService.list();
});

// 有返回值
List<YourEntity> result = DataPermissionHelper.ignore(() -> {
    return yourService.list();
});
```

**使用场景**:
- 系统管理功能
- 统计分析
- 数据导出
- 超级管理员操作

### 3. 与多租户同时忽略

```java
@InterceptorIgnore(tenantLine = "true", dataPermission = "true")
List<YourEntity> selectAllData();
```

## 配置步骤完整示例

### 场景: 用户只能看到本部门及子部门的订单

#### Step 1: 设计数据表

```sql
CREATE TABLE `business_order` (
  `id` bigint NOT NULL,
  `order_no` varchar(50) NOT NULL,
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `user_id` bigint DEFAULT NULL COMMENT '创建人ID',
  `tenant_id` varchar(20) DEFAULT '000000',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) COMMENT='业务订单表';
```

#### Step 2: 创建 Entity

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("business_order")
public class BusinessOrder extends TenantEntity {
    /**
     * 订单ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 创建人ID
     */
    private Long userId;
}
```

#### Step 3: 创建 Bo 和 Vo

```java
@Data
public class BusinessOrderBo extends PageQuery {
    private String orderNo;
    // 其他查询字段...
}

@Data
public class BusinessOrderVo {
    private Long id;
    private String orderNo;
    private Long deptId;
    private Long userId;
    // 其他展示字段...
}
```

#### Step 4: 创建 Mapper

```java
@DataPermission({
    @DataColumn(key = "deptName", value = "dept_id"),
    @DataColumn(key = "userId", value = "user_id")
})
@Mapper
public interface BusinessOrderMapper extends BaseMapperPlus<BusinessOrder, BusinessOrderVo> {
    // 类级别注解，所有方法自动带数据权限
}
```

#### Step 5: 创建 Service

```java
@Service
public class BusinessOrderServiceImpl extends ServiceImpl<BusinessOrderMapper, BusinessOrder>
    implements IBusinessOrderService {

    @Override
    public Page<BusinessOrderVo> selectPageList(BusinessOrderBo bo) {
        // 自动应用数据权限
        return this.page(PageQuery.build(), buildQueryWrapper(bo));
    }

    @Override
    @DataPermission({
        @DataColumn(key = "userId", value = "user_id")
    })
    public List<BusinessOrderVo> selectMyOrders() {
        // 方法级别覆盖，只过滤用户
        return this.list();
    }
}
```

#### Step 6: 配置角色数据权限

1. 进入系统管理 → 角色管理
2. 新建/编辑角色
3. 数据权限选择 "本部门及以下"
4. 保存

#### Step 7: 测试

```java
// 测试代码
@Test
void testDataPermission() {
    // 使用部门经理角色登录
    LoginHelper.login(deptManagerUser);

    Page<BusinessOrderVo> page = orderService.selectPageList(new BusinessOrderBo());

    // 只能看到本部门及以下的数据
    assertThat(page.getRecords()).allMatch(order ->
        order.getDeptId().equals(deptManagerUser.getDeptId()) ||
        isChildDept(order.getDeptId(), deptManagerUser.getDeptId())
    );
}
```

## 常见问题

### 问题 1: 数据权限不生效

**可能原因**:
1. 注解不在 Mapper 层
2. 最终执行的 Mapper 方法没有注解
3. 使用了自定义 SQL 绕过拦截器

**解决方案**:
```java
// ✅ 正确: 在 Mapper 层添加注解
@Mapper
public interface YourMapper {
    @DataPermission({...})
    List<YourEntity> selectList(YourBo bo);
}

// ❌ 错误: 在 Service 层添加注解
@Service
public class YourService {
    @DataPermission({...}) // 无效
    List<YourEntity> selectList(YourBo bo) { ... }
}
```

### 问题 2: MyBatis-Plus 原生方法无数据权限

**解决方案**: 添加 default 方法包装

```java
@Mapper
public interface YourMapper extends BaseMapperPlus<YourEntity, YourVo> {

    @DataPermission({...})
    default Page<YourVo> selectPageWithPermission(Page<YourVo> page, YourBo bo) {
        return this.selectPage(page, buildLambdaQueryWrapper(bo));
    }
}
```

### 问题 3: 类级别注解与方法级别冲突

**规则**: 方法 > 类

```java
@DataPermission({
    @DataColumn(key = "deptName", value = "dept_id")
})
@Mapper
public interface YourMapper {
    // 使用类级别注解: dept_id

    @DataPermission({
        @DataColumn(key = "userId", value = "user_id")
    })
    List<YourEntity> selectList(); // 使用方法注解: user_id
}
```

### 问题 4: 更新/删除无数据权限

**原因**: 没有在方法上添加数据权限注解

**解决方案**:
```java
@Mapper
public interface YourMapper {
    @DataPermission({...})
    int updateById(YourEntity entity);

    @DataPermission({...})
    int deleteById(Long id);
}
```

## 数据权限与多租户

### 同时使用

```java
@Mapper
public interface YourMapper extends BaseMapperPlus<YourEntity, YourVo> {

    // 租户过滤自动生效，数据权限额外过滤
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    List<YourEntity> selectList(YourBo bo);
}
```

### 忽略两者

```java
@InterceptorIgnore(tenantLine = "true", dataPermission = "true")
List<YourEntity> selectAll();
```

## 最佳实践

### 1. 数据权限设计原则

- ✅ 在 Mapper 层使用注解
- ✅ 使用类级别注解统一管理
- ✅ 特殊方法使用方法级别注解覆盖
- ✅ 自定义模板放在 DataScopeType

### 2. 性能优化

- ✅ 合理使用索引（dept_id, user_id）
- ✅ 避免过度复杂的 SQL 模板
- ✅ 使用缓存减少查询

### 3. 安全建议

- ✅ 默认开启数据权限
- ✅ 使用兜底 SQL（elseSql = "1 = 0"）
- ✅ 严格管理数据权限配置
- ✅ 定期审计数据权限规则

## 参考资源

- [RuoYi-Cloud-Plus 数据权限文档](https://plus-doc.dromara.org/ruoyi-cloud-plus/framework/basic/permissions.html)
- [MyBatis-Plus 拦截器](https://baomidou.com/pages/2976a3/)

## 版本历史

- v1.0.0 (2026-01-17) - 初始版本，基于 RuoYi-Cloud-Plus 2.3.X
