# Dragonboat-Backend Code Review Standards

Dragonboat-Backend 项目特定的代码审查标准，基于 RuoYi-Cloud-Plus 框架架构和 DDD 设计原则。

## 触发词

- "代码审查"
- "code review"
- "review code"
- "审查代码"
- "检查代码"
- "run review"
- "自动审查"

## 审查分类

审查问题分为三个级别:

| 级别 | 描述 | 示例 | 必须修复 |
|------|------|------|----------|
| **Critical** | 严重问题，必须立即修复 | SQL 注入、密码明文存储 | 是 |
| **Major** | 重要问题，强烈建议修复 | 缺少输入验证、空指针风险 | 是 |
| **Minor** | 次要问题，建议修复 | 缺少注释、命名不规范 | 否 |

## 审查检查项

### 1. DDD 分层架构检查

#### 1.1 分层结构

**正确示例**:
```
ruoyi-modules/system/
├── controller/SystemUserController.java    # HTTP 请求处理
├── service/
│   ├── ISystemUserService.java            # 服务接口
│   └── impl/SystemUserServiceImpl.java    # 服务实现
├── mapper/SystemUserMapper.java           # 数据访问
└── domain/
    ├── SystemUser.java                    # 实体
    ├── bo/SystemUserBo.java               # 业务对象
    ├── vo/SystemUserVo.java               # 视图对象
    └ convert/SystemUserConvert.java       # 转换器
```

**检查项**:
- [ ] Controller 只处理 HTTP 请求/响应
- [ ] Service 包含业务逻辑
- [ ] Mapper 只负责数据库操作
- [ ] Entity/Bo/Vo 职责清晰

**常见问题**:

| 问题 | 级别 | 描述 |
|------|------|------|
| Controller 包含业务逻辑 | Major | 业务逻辑应放在 Service 层 |
| Service 直接操作 HTTP | Major | HTTP 操作应在 Controller 层 |
| 跨层调用 | Major | Controller 不应直接调用 Mapper |
| 缺少 Bo/Vo | Minor | 应使用 Bo/Vo 隔离输入输出 |

#### 1.2 依赖方向

**规则**: 上层可以依赖下层，下层不能依赖上层

```
Controller -> Service -> Mapper
    |          |          |
    v          v          v
    Vo <------ Bo <----- Entity
```

**检查项**:
- [ ] 不允许循环依赖
- [ ] Service 不依赖 Controller
- [ ] Mapper 不依赖 Service

### 2. 微服务标准检查

#### 2.1 Dubbo API 规范

**正确示例**:
```java
// API 接口位置: ruoyi-api/ruoyi-api-system/
package org.dromara.system.api;

public interface RemoteUserService {
    R<LoginUser> getUserInfo(String username, String source);
}
```

**检查项**:
- [ ] API 接口在 `ruoyi-api` 模块
- [ ] 返回类型统一使用 `R<T>`
- [ ] 使用 `@RemoteService` 注解
- [ ] Mock 实现已实现

**常见问题**:

| 问题 | 级别 | 描述 |
|------|------|------|
| API 接口在 modules 中 | Critical | API 必须在 ruoyi-api 模块 |
| 返回类型非 `R<T>` | Major | 统一使用 R 包装 |
| 缺少 Mock 实现 | Major | 必须提供 Mock 降级 |

#### 2.2 多租户规范

**正确示例**:
```java
// 实体继承 TenantEntity
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends TenantEntity {
    // 租户隔离的配置表
}

// 非租户表
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    // 全局用户表
}
```

**检查项**:
- [ ] 租户表继承 `TenantEntity`
- [ ] 非租户表继承 `BaseEntity`
- [ ] 租户字段自动填充
- [ ] 租户隔离逻辑正确

**常见问题**:

| 问题 | 级别 | 描述 |
|------|------|------|
| 租户表用 BaseEntity | Critical | 应用 TenantEntity |
| 手动设置 tenantId | Major | 应自动填充 |
| 缺少租户隔离 | Critical | 数据隔离漏洞 |

#### 2.3 服务发现与配置

**检查项**:
- [ ] Dubbo 服务正确注册
- [ ] Nacos 配置正确
- [ ] 版本号管理规范

### 3. 安全与权限控制

#### 3.1 认证授权

**正确示例**:
```java
// Controller 使用注解
@SaIgnore  // 公开接口
@GetMapping("/public/info")
public R<UserInfo> getPublicInfo() {}

@SaCheckPermission("system:user:list")  // 权限检查
@GetMapping("/list")
public TableDataInfo<SysUserVo> list() {}

@SaCheckRole("admin")  // 角色检查
@PostMapping("/add")
public R<Void> add() {}
```

**检查项**:
- [ ] 公开接口使用 `@SaIgnore`
- [ ] 敏感操作使用 `@SaCheckPermission`
- [ ] 管理员操作使用 `@SaCheckRole`
- [ ] 资源所有权验证

**常见问题**:

| 问题 | 级别 | 描述 |
|------|------|------|
| 敏感接口无权限检查 | Critical | 安全漏洞 |
| 使用字符串硬编码权限 | Major | 应用常量 |
| 缺少资源所有权验证 | Critical | 越权漏洞 |

#### 3.2 数据加密

**正确示例**:
```java
public class SysUser extends BaseEntity {

    @EncryptField(algorithm = AlgorithmType.AES)
    private String password;

    @EncryptField(algorithm = AlgorithmType.SM4)
    private String idCard;
}
```

**检查项**:
- [ ] 密码字段使用 `@EncryptField`
- [ ] 敏感信息（手机、身份证）加密
- [ ] 加密算法符合规范

**常见问题**:

| 问题 | 级别 | 描述 |
|------|------|------|
| 密码明文存储 | Critical | 严重安全漏洞 |
| 敏感信息未加密 | Critical | 数据泄露风险 |

#### 3.3 输入验证

**正确示例**:
```java
@PostMapping("/add")
public R<Void> add(@Validated @RequestBody SysUserBo bo) {
    // Bo 类中的验证
    public class SysUserBo {

        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 20, message = "用户名长度2-20字符")
        private String userName;

        @Pattern(regexp = RegexConstants.MOBILE, message = "手机号格式错误")
        private String phonenumber;
    }
}
```

**检查项**:
- [ ] Bo 类使用验证注解
- [ ] Controller 使用 `@Validated`
- [ ] 自定义验证注解正确
- [ ] 错误消息清晰

### 4. 数据层规范

#### 4.1 MyBatis-Plus 规范

**正确示例**:
```java
// Mapper 继承 BaseMapperPlus
public interface SysUserMapper extends BaseMapperPlus<SysUser, SysUserVo> {

    // 自定义方法使用注解
    @Select("SELECT * FROM sys_user WHERE user_id = #{userId}")
    SysUserVo selectById(@Param("userId") Long userId);
}
```

**检查项**:
- [ ] Mapper 继承 `BaseMapperPlus`
- [ ] 使用泛型指定 Entity 和 Vo
- [ ] 避免使用 XML 配置（优先注解）

#### 4.2 数据权限

**正确示例**:
```java
// Service 使用数据权限注解
@DataPermission(...)
public List<SysUserVo> selectList(SysUserBo bo) {
    // 查询结果自动过滤
}
```

**检查项**:
- [ ] 使用 `@DataPermission` 注解
- [ ] 数据范围配置正确
- [ ] 避免绕过数据权限

#### 4.3 事务管理

**正确示例**:
```java
@Service
public class SystemUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
    implements ISystemUserService {

    @Transactional(rollbackFor = Exception.class)
    public Boolean insertUser(SysUserBo bo) {
        // 事务方法
    }
}
```

**检查项**:
- [ ] 涉及多表操作使用 `@Transactional`
- [ ] 指定 `rollbackFor = Exception.class`
- [ ] 避免长事务

**常见问题**:

| 问题 | 级别 | 描述 |
|------|------|------|
| 缺少事务注解 | Major | 数据一致性风险 |
| 事务过大 | Minor | 性能问题 |

### 5. 代码质量检查

#### 5.1 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | PascalCase | `SystemUserController` |
| 方法名 | camelCase | `getUserById` |
| 常量 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| 包名 | lowercase | `org.dromara.system` |

#### 5.2 注释规范

**检查项**:
- [ ] 公共 API 有 JavaDoc
- [ ] 复杂逻辑有注释
- [ ] 避免 TODO 未清理
- [ ] 避免注释掉的代码

#### 5.3 代码异味检测

**常见问题**:

| 问题 | 级别 | 描述 |
|------|------|------|
| System.out.println | Minor | 应用日志框架 |
| 硬编码配置 | Major | 应用配置文件 |
| 过长方法 (>50行) | Minor | 需拆分 |
| 重复代码 | Minor | 需提取 |
| 空捕获块 | Major | 吞噬异常 |

### 6. 测试覆盖

**检查项**:
- [ ] 核心业务逻辑有单元测试
- [ ] Controller 有集成测试
- [ ] 测试覆盖率 > 70%
- [ ] 测试命名清晰

### 7. RuoYi-Cloud-Plus 框架特性检查

#### 7.1 多租户检查

**检查项**:
- [ ] 租户表包含 `tenant_id` 字段
- [ ] Entity 继承 `TenantEntity`（租户表）或 `BaseEntity`（非租户表）
- [ ] 非租户表配置到 `application-common.yml` 的 `excludes`
- [ ] 避免手动拼接 SQL 绕过多租户过滤
- [ ] 特殊场景使用 `TenantHelper.ignore()` 或 `TenantHelper.dynamic()`

**常见问题**:

| 问题 | 级别 | 描述 |
|------|------|------|
| 租户表缺少 tenant_id | Critical | 数据隔离漏洞 |
| 租户表未继承 TenantEntity | Critical | 多租户不生效 |
| 手动拼接 SQL | Major | 绕过多租户过滤 |
| 滥用 TenantHelper.ignore() | Major | 数据泄露风险 |

#### 7.2 数据权限检查

**检查项**:
- [ ] `@DataPermission` 注解在 Mapper 层使用（不在 Service 层）
- [ ] 类级别注解与方法级别注解优先级正确（方法 > 类）
- [ ] `@DataColumn` 的 `key` 对应模板变量，`value` 对应数据库字段
- [ ] `@InterceptorIgnore(dataPermission = "true", tenantLine = "true")` 一起使用
- [ ] MyBatis-Plus 原生方法通过 default 方法包装添加数据权限

**常见问题**:

| 问题 | 级别 | 描述 |
|------|------|------|
| 数据权限注解在 Service 层 | Critical | 不生效 |
| @InterceptorIgnore 单独使用 | Major | 数据权限失效 |
| 忘记包装 MP 原生方法 | Major | 数据权限不生效 |

#### 7.3 多数据源检查

**检查项**:
- [ ] `@DS` 注解在 Service 或 Mapper 层使用（不在 Controller 层）
- [ ] 本地多数据源事务使用 `@DSTransactional`（非 `@Transactional`）
- [ ] 分布式事务使用 `@GlobalTransactional`
- [ ] 避免混用 `@DSTransactional` 和 `@Transactional`
- [ ] 手动切换数据源使用 try-finally 清除

**常见问题**:

| 问题 | 级别 | 描述 |
|------|------|------|
| @DS 注解在 Controller 层 | Major | 不生效 |
| 混用事务注解 | Critical | 事务失效 |
| 忘记清除手动切换 | Major | 数据源错误 |

#### 7.4 分页检查

**检查项**:
- [ ] Controller 使用 `PageQuery` 接收分页参数
- [ ] 使用 `PageQuery#build()` 构建 MP 分页对象
- [ ] 自定义 SQL 分页第一个参数是分页对象
- [ ] 避免使用 `PageHelper`（应使用 MyBatis-Plus 分页插件）

**常见问题**:

| 问题 | 级别 | 描述 |
|------|------|------|
| 未使用 PageQuery | Minor | 不符合框架规范 |
| 自定义 SQL 分页参数错误 | Major | 分页不生效 |

#### 7.5 Excel 导入导出检查

**检查项**:
- [ ] 对象不使用 `@Accessors(chain = true)`（EasyExcel 限制）
- [ ] 使用 `@ExcelProperty` 标注导出字段
- [ ] 使用 `@ExcelDictFormat` 或 `@ExcelEnumFormat` 实现翻译
- [ ] 自定义转换器实现 `Converter` 接口

**常见问题**:

| 问题 | 级别 | 描述 |
|------|------|------|
| 使用 @Accessors(chain = true) | Critical | Excel 导出错误 |

#### 7.6 Sa-Token 权限检查

**检查项**:
- [ ] 公开接口使用 `@SaIgnore`
- [ ] 权限检查使用 `@SaCheckPermission("system:user:list")`
- [ ] 角色检查使用 `@SaCheckRole("admin")`
- [ ] 复杂表达式使用 `mode = SaMode.OR` 或 `SaMode.AND`
- [ ] 角色权限双重校验使用 `orRole` 参数

**常见问题**:

| 问题 | 级别 | 描述 |
|------|------|------|
| 敏感接口无权限检查 | Critical | 安全漏洞 |
| 使用字符串硬编码权限 | Major | 应使用常量 |

#### 7.7 代码生成规范检查

**检查项**:
- [ ] Mapper 继承 `BaseMapperPlus<Entity, Vo>`
- [ ] Service 继承 `ServiceImpl<Mapper, Entity>`
- [ ] 使用 `@TableId(type = IdType.ASSIGN_ID)` 雪花ID
- [ ] 避免使用主子表生成（建议单表生成）
- [ ] Entity 不使用 `@Accessors(chain = true)`

#### 7.8 客户端管理检查

**检查项**:
- [ ] 前端请求头携带 `clientid`
- [ ] 不同客户端 Token 不互通
- [ ] 不要删除默认客户端数据
- [ ] 新增登录方式实现 `IAuthStrategy` 接口

#### 7.9 缓存使用检查

**检查项**:
- [ ] 使用 `@Cacheable` 注解缓存数据
- [ ] 缓存 Key 自动包含租户ID
- [ ] 使用 `@CacheEvict` 清除缓存
- [ ] 避免 `redisTemplate.keys()` 使用（应使用 `scan`）

**常见问题**:

| 问题 | 级别 | 描述 |
|------|------|------|
| 使用 keys 命令 | Major | 性能问题 |
| 缓存未清除导致数据不一致 | Major | 数据错误 |

#### 7.10 OSS 文件存储检查

**检查项**:
- [ ] 访问站点不包含 `http` 前缀
- [ ] MinIO 使用 `127.0.0.1` 而非 `localhost`
- [ ] 云厂商绑定自定义域名（强烈建议）
- [ ] 七牛云必须绑定域名

#### 7.11 内网鉴权检查

**检查项**:
- [ ] 生产环境开启内网鉴权 `check-id-token: true`
- [ ] 公开接口配置到放行路径
- [ ] 避免 `InterceptorIgnore` 滥用

**常见问题**:

| 问题 | 级别 | 描述 |
|------|------|------|
| 生产环境关闭内网鉴权 | Critical | 安全漏洞 |
| 放行路径过多 | Major | 增加攻击面 |

## 审查输出格式

```markdown
## Code Review Report

**Branch**: feat/DB-101-user-auth
**Commit**: abc123f
**Files**: 12
**Lines**: +850, -120

### Summary

| Level | Count | Status |
|-------|-------|--------|
| Critical | 0 | ✓ PASS |
| Major | 2 | ⚠ NEEDS ATTENTION |
| Minor | 5 | ✓ ACCEPTABLE |

### Critical Issues

None

### Major Issues

#### 1. Password should use @EncryptField

**File**: `ruoyi-modules/system/domain/SysUser.java:45`
**Severity**: Major
**Description**: 密码字段应使用 @EncryptField 注解进行加密

```java
// Current
private String password;

// Suggested
@EncryptField(algorithm = AlgorithmType.AES)
private String password;
```

#### 2. Missing input validation

**File**: `ruoyi-modules/system/domain/bo/SysUserBo.java:30`
**Severity**: Major
**Description**: 用户名字段缺少验证注解

```java
// Current
private String userName;

// Suggested
@NotBlank(message = "用户名不能为空")
@Size(min = 2, max = 20, message = "用户名长度2-20字符")
private String userName;
```

### Minor Issues

#### 1. Add JavaDoc for public method

**File**: `ruoyi-modules/system/service/impl/SystemUserServiceImpl.java:120`
**Severity**: Minor
**Description**: 公共方法应添加 JavaDoc 注释

#### 2. Remove System.out.println

**File**: `ruoyi-modules/system/controller/SystemUserController.java:85`
**Severity**: Minor
**Description**: 应使用日志框架代替 System.out.println

```java
// Current
System.out.println("User logged in: " + username);

// Suggested
log.info("User logged in: {}", username);
```

### Architecture Review

| Category | Status | Notes |
|----------|--------|-------|
| DDD Layering | ✓ PASS | 分层结构清晰 |
| Dubbo API | ✓ PASS | API 规范符合 |
| Security | ⚠ WARNING | 需修复加密问题 |
| Data Access | ✓ PASS | MyBatis-Plus 规范 |
| Testing | ✓ PASS | 覆盖率 85% |

### Recommendations

1. **必须修复**: 密码加密和输入验证问题
2. **建议**: 添加更多单元测试覆盖边界情况
3. **优化**: 考虑将部分业务逻辑提取到领域服务

### Next Steps

1. 修复 Critical 和 Major 问题
2. 提交修复
3. 重新触发审查
4. 确认无误后合并

---

**Review Time**: 2026-01-17 14:30:00
**Reviewer**: Claude Code
**Auto-Generated**: Yes
```

## 自动审查命令

```bash
# 审查当前分支
/skill ry-code-review

# 审查指定文件
/skill ry-code-review ruoyi-modules/system/

# 审查最近3次提交
/skill ry-code-review --commits 3

# 生成详细报告
/skill ry-code-review --detailed
```

## 配置

审查规则可在以下位置自定义:

- `.claude/skills/ry-code-review.md` - 审查标准
- `.github/workflows/auto-code-review.yml` - GitHub Actions 配置

## 版本历史

- v1.0.0 (2026-01-17) - 初始版本
