# Dragonboat-Backend Workflow Orchestrator

Dragonboat-Backend 项目基于 RuoYi-Cloud-Plus 框架的工作流总入口，协调所有开发流程相关 skills。

## 触发词

- "开始新功能"
- "开始新任务"
- "创建功能分支"
- "完成开发"
- "准备 PR"
- "创建 PR"
- "start feature"
- "create feature"
- "ready for review"
- "create PR"
- "开发流程"
- "workflow"

## 工作流概览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Dragonboat-Backend 开发工作流                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    │
│  │需求分析  │ -> │设计阶段  │ -> │开发阶段  │ -> │测试阶段  │ -> │发布阶段  │    │
│  └─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘    │
│       │             │             │             │             │             │
│       v             v             v             v             v             │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    │
│  │创建分支  │    │数据库   │    │代码生成  │    │代码审查  │    │创建PR   │    │
│  │branch   │    │设计     │    │generator│    │review   │    │merge    │    │
│  └─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘    │
│                                                                             │
│  相关 Skills:                                                                │
│  - ry-branch-management (分支管理)                                          │
│  - ry-multi-tenant (多租户开发)                                              │
│  - ry-data-permission (数据权限)                                            │
│  - ry-code-generator (代码生成)                                             │
│  - ry-crud (CRUD开发)                                                       │
│  - ry-dubbo-api (Dubbo API)                                                │
│  - ry-code-review (代码审查)                                                │
│  - ry-commit-standards (Commit规范)                                        │
│  - ry-custom-auth (自定义登录)                                              │
│  - ry-multi-datasource (多数据源)                                           │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 快速开始

### 最简开发流程

```bash
# 1. 开始新功能（自动创建分支）
/skill ry-workflow "DB-101 用户管理"

# 2. 设计数据库表（考虑多租户、数据权限）
# 参考: ry-multi-tenant, ry-data-permission

# 3. 使用代码生成器生成代码
# 参考: ry-code-generator

# 4. 实现业务逻辑
# 参考: ry-crud, ry-dubbo-api

# 5. 提交代码
/skill commit-commands:commit

# 6. 创建 PR
/skill create-pr

# 7. 等待代码审查和 CI 通过
```

## 完整开发流程详解

### 阶段 0: 需求分析

**目标**: 明确功能需求和技术方案

**检查清单**:
- [ ] 功能需求清晰
- [ ] 确定涉及的模块和服务
- [ ] 确定是否需要新微服务
- [ ] 确定数据源配置
- [ ] 确定是否涉及多租户
- [ ] 确定数据权限需求
- [ ] 确定是否需要自定义登录

**相关 Skills**:
- `/skill ry-module-create` - 创建新微服务
- `/skill ry-multi-tenant` - 多租户开发指导
- `/skill ry-data-permission` - 数据权限配置
- `/skill ry-custom-auth` - 自定义登录

### 阶段 1: 分支创建

**触发**: "开始新功能 DB-101" 或 `/skill ry-workflow "DB-101"`

**执行步骤**:

```bash
# 1. 确保在 2.X 分支
git checkout 2.X
git pull origin 2.X

# 2. 创建功能分支
git checkout -b feat/DB-101-user-management

# 3. 推送到远程
git push -u origin feat/DB-101-user-management
```

**分支命名规范**:
```
<type>/<ID>-<description>

示例:
feat/DB-101-user-management
fix/DB-205-login-error
refactor/DB-301-cache-optimization
docs/DB-401-api-documentation
```

**相关 Skills**:
- `/skill ry-branch-management` - 分支管理详细指导

### 阶段 2: 数据库设计

**目标**: 设计符合 RuoYi-Cloud-Plus 规范的数据库表

**设计检查清单**:

#### 2.1 基础字段检查

```sql
-- 必备字段
`id` bigint NOT NULL COMMENT '主键ID'
`create_dept` bigint DEFAULT NULL COMMENT '创建部门'
`create_by` bigint DEFAULT NULL COMMENT '创建人'
`create_time` datetime DEFAULT NULL COMMENT '创建时间'
`update_by` bigint DEFAULT NULL COMMENT '更新人'
`update_time` datetime DEFAULT NULL COMMENT '更新时间'

-- 可选字段
`tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号'
`del_flag` char(1) DEFAULT '0' COMMENT '删除标志'
`version` int DEFAULT 0 COMMENT '乐观锁版本号'
```

#### 2.2 多租户检查

```sql
-- 租户表必须包含
`tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号'

-- 相关配置
-- Entity: 继承 TenantEntity
-- 配置: application-common.yml 的 tenant.excludes 排除非租户表
```

**相关 Skills**:
- `/skill ry-multi-tenant` - 多租户完整指导

#### 2.3 数据权限字段

```sql
-- 部门数据权限
`dept_id` bigint DEFAULT NULL COMMENT '部门ID'

-- 用户数据权限
`user_id` bigint DEFAULT NULL COMMENT '创建人ID'
```

**相关 Skills**:
- `/skill ry-data-permission` - 数据权限完整指导

#### 2.4 主键配置

```sql
-- 使用雪花ID
`id` bigint NOT NULL COMMENT '主键ID'

-- Entity 配置
@TableId(type = IdType.ASSIGN_ID)
```

### 阶段 3: 代码生成

**目标**: 使用代码生成器快速生成 CRUD 代码

**操作流程**:

```
1. 访问系统工具 → 代码生成
2. 切换数据源（如果需要）
3. 导入数据表
4. 编辑表生成信息
   - 基本信息（模块、业务名称、作者）
   - 字段配置（插入、编辑、查询、列表）
   - 显示类型配置
5. 预览生成的代码
6. 生成代码（下载或直接生成）
```

**字段配置要点**:

| 字段类型 | 插入 | 编辑 | 列表 | 查询 | 必填 |
|---------|:---:|:---:|:---:|:---:|:---:|
| 主键ID | ❌ | ❌ | ✅ | ❌ | ❌ |
| 租户ID | ❌ | ❌ | ❌ | ❌ | ❌ |
| 业务字段 | ✅ | ✅ | ✅ | ✅ | 按需 |
| 创建信息 | ❌ | ❌ | ❌ | ❌ | ❌ |
| 更新信息 | ❌ | ❌ | ❌ | ❌ | ❌ |

**相关 Skills**:
- `/skill ry-code-generator` - 代码生成器详细指导

### 阶段 4: 业务实现

**目标**: 实现具体的业务逻辑

#### 4.1 检查生成的代码

```bash
# 检查文件结构
ls -la ruoyi-modules/{module}/domain/
ls -la ruoyi-modules/{module}/mapper/
ls -la ruoyi-modules/{module}/service/
ls -la ruoyi-modules/{module}/controller/
```

#### 4.2 调整业务逻辑

**Entity 调整**:
```java
// ✅ 租户表
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("your_table")
public class YourEntity extends TenantEntity {
    // 租户相关字段已在 TenantEntity 中
}

// ✅ 非租户表
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_global_config")
public class SysGlobalConfig extends BaseEntity {
    // 全局配置字段
}
```

**Bo 调整**:
```java
@Data
public class YourBo extends PageQuery {
    // 添加验证注解
    @NotBlank(message = "名称不能为空")
    @Size(min = 2, max = 50, message = "名称长度2-50字符")
    private String name;

    // 添加自定义查询字段
    private String customField;
}
```

**Vo 调整**:
```java
@Data
public class YourVo {
    // 添加格式化注解
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 添加脱敏注解
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    private String phonenumber;
}
```

**Service 实现**:
```java
@Service
public class YourServiceImpl
    extends ServiceImpl<YourMapper, YourEntity>
    implements IYourService {

    // 添加数据权限
    @Override
    public Page<YourVo> selectPageList(YourBo bo) {
        return this.page(PageQuery.build(), buildQueryWrapper(bo));
    }
}
```

**Controller 实现**:
```java
@RestController
@RequestMapping("/system/your")
public class YourController {

    // 添加权限检查
    @SaCheckPermission("system:your:list")
    @GetMapping("/list")
    public TableDataInfo<YourVo> list(YourBo bo) {
        return yourService.selectPageList(bo);
    }
}
```

**Mapper 实现**:
```java
@DataPermission({
    @DataColumn(key = "deptName", value = "dept_id"),
    @DataColumn(key = "userId", value = "user_id")
})
@Mapper
public interface YourMapper extends BaseMapperPlus<YourEntity, YourVo> {
    // 类级别数据权限
}
```

**相关 Skills**:
- `/skill ry-crud` - CRUD 开发详细指导
- `/skill ry-dubbo-api` - Dubbo API 开发
- `/skill ry-multi-datasource` - 多数据源配置
- `/skill ry-custom-auth` - 自定义登录

### 阶段 5: 本地测试

**目标**: 确保功能正常

**测试检查清单**:

#### 5.1 功能测试

- [ ] 列表查询正常
- [ ] 详情查询正常
- [ ] 新增功能正常
- [ ] 编辑功能正常
- [ ] 删除功能正常
- [ ] 导入导出正常

#### 5.2 租户隔离测试（多租户表）

```java
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

#### 5.3 数据权限测试

```java
@Test
void testDataPermission() {
    // 部门经理登录
    LoginHelper.login(deptManager);

    // 只能看到本部门数据
    Page<YourVo> page = yourService.selectPageList(new YourBo());

    assertThat(page.getRecords()).allMatch(item ->
        item.getDeptId().equals(deptManager.getDeptId())
    );
}
```

#### 5.4 多数据源测试

```java
@Test
void testMultiDatasource() {
    // 主数据源操作
    @DS("master")
    masterService.insertData();

    // 从数据源操作
    @DS("slave")
    slaveService.queryData();
}
```

**相关 Skills**:
- `/skill ry-multi-tenant` - 租户测试要点
- `/skill ry-data-permission` - 权限测试要点

### 阶段 6: 代码提交

**触发**: `/skill commit-commands:commit`

**提交前检查清单**:

```bash
# 1. 检查文件状态
git status

# 2. 检查分支
git branch --show-current

# 3. 格式化代码（IDEA: Ctrl+Alt+L）

# 4. 运行测试
mvn test

# 5. 提交代码
git add .
git commit -m "feat(scope): subject

- 详细描述

Closes DB-XXX"
```

**Commit Message 格式**:
```
<type>(<scope>): <subject>

<body>

Closes <issue>
```

**相关 Skills**:
- `/skill ry-commit-standards` - Commit 规范详细指导

### 阶段 7: 代码审查

**触发**: PR 创建后自动触发

**审查范围**:

#### 7.1 框架特性检查

- [ ] 多租户: 租户表继承 TenantEntity，非租户表继承 BaseEntity
- [ ] 数据权限: @DataPermission 在 Mapper 层使用
- [ ] 多数据源: @DS 注解使用正确，事务注解正确
- [ ] 分页: 使用 PageQuery 和 PageQuery#build()
- [ ] 权限: @SaCheckPermission/@SaCheckRole 使用正确
- [ ] 加密: 密码等敏感字段使用 @EncryptField
- [ ] 脱敏: 敏感字段使用 @Sensitive
- [ ] 验证: Bo 类使用验证注解
- [ ] Excel: 不使用 @Accessors(chain = true)

#### 7.2 架构检查

- [ ] DDD 分层正确
- [ ] Dubbo API 规范
- [ ] 代码命名规范
- [ ] 注释完整

**相关 Skills**:
- `/skill ry-code-review` - 完整审查标准

### 阶段 8: 创建 PR

**触发**: `/skill create-pr`

**PR 描述模板**:

```markdown
## Summary
- [ ] 实现 DB-XXX 功能描述
- [ ] 添加 xxx 功能
- [ ] 集成 xxx 组件

## 技术实现
- 框架版本: RuoYi-Cloud-Plus 2.X
- 涉及模块: xxx
- 数据源: xxx (默认/多数据源)
- 多租户: 是/否
- 数据权限: 是/否

## Test plan
- [ ] 本地功能测试通过
- [ ] 单元测试覆盖率 > 70%
- [ ] 租户隔离测试通过（如涉及）
- [ ] 数据权限测试通过（如涉及）
- [ ] 集成测试通过

## Checklist
- [ ] 遵循 DDD 分层架构
- [ ] 代码通过格式化检查
- [ ] 添加必要的 JavaDoc 注释
- [ ] 敏感字段已加密/脱敏
- [ ] 权限检查已添加
- [ ] 事务注解使用正确
- [ ] 无 System.out.println
- [ ] 无硬编码配置

## Related Issues
Closes DB-XXX

## Screenshots (if applicable)
[在此添加截图]

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

### 阶段 9: 审查修复

**触发**: 审查发现问题

**修复流程**:

1. 查看 GitHub Actions 报告
2. 查看自动代码审查评论
3. 修复 Critical 和 Major 问题
4. 提交修复
5. 等待重新审查

**修复提交格式**:
```
fix(auth): add missing @EncryptField for password field

- Update review comments
- Fix critical security issues

Ref for review comments
```

### 阶段 10: 合并发布

**触发**: 审查通过

**合并流程**:

```bash
# 1. 确认 CI 全部通过
# 2. 确认审查通过
# 3. 合并 PR (Squash and Merge)
# 4. 删除本地分支
git branch -d feat/DB-101-feature
# 5. 删除远程分支
git push origin --delete feat/DB-101-feature
```

## 常见开发场景

### 场景 1: 新增业务模块（在现有服务中）

```
1. /skill ry-workflow "DB-101 新增用户管理"
2. 设计数据库表（考虑多租户、数据权限）
3. /skill ry-code-generator 生成代码
4. 调整业务逻辑
5. 本地测试
6. /skill commit-commands:commit
7. /skill create-pr
8. 等待审查通过
9. 合并
```

### 场景 2: 新增微服务

```
1. 需求分析（确定是否需要新服务）
2. /skill ry-module-create 创建新服务
3. 配置 Nacos
4. 配置 Gateway 路由
5. 配置 Seata（如需要）
6. /skill ry-code-generator 生成代码
7. 实现业务逻辑
8. 本地测试
9. /skill commit-commands:commit
10. /skill create-pr
11. 等待审查通过
12. 合并
```

**相关 Skills**:
- `/skill ry-module-create` - 创建新微服务

### 场景 3: 修改框架配置

```
1. 确认配置修改范围
2. 检查影响的服务
3. 本地测试配置
4. 提交配置修改
5. /skill create-pr
6. 说明配置变更原因
7. 等待审查通过
8. 合并后重启相关服务
```

### 场景 4: 多数据源业务开发

```
1. 需求分析（确定涉及的数据源）
2. 配置数据源（application-common.yml）
3. /skill ry-code-generator 生成代码
4. Service 使用 @DS 注解
5. 选择正确的事务注解
6. 本地测试数据源切换
7. 本地测试事务
8. /skill commit-commands:commit
9. /skill create-pr
10. 说明数据源使用情况
11. 等待审查通过
12. 合并
```

**相关 Skills**:
- `/skill ry-multi-datasource` - 多数据源详细指导

### 场景 5: 自定义登录开发

```
1. 需求分析（确定登录方式）
2. 新增字典数据（授权类型）
3. 修改客户端授权类型
4. 实现 IAuthStrategy 接口
5. 定义 LoginBody 参数
6. 配置校验分组
7. 本地测试登录流程
8. /skill commit-commands:commit
9. /skill create-pr
10. 等待审查通过
11. 合并
```

**相关 Skills**:
- `/skill ry-custom-auth` - 自定义登录详细指导

## 开发检查清单

### 开发前检查

- [ ] 需求分析完成
- [ ] 技术方案明确
- [ ] 涉及的服务确定
- [ ] 数据源配置明确
- [ ] 多租户需求明确
- [ ] 数据权限需求明确

### 提交前检查

- [ ] 代码格式化完成
- [ ] 注释完整（JavaDoc）
- [ ] 敏感字段已加密
- [ ] 敏感字段已脱敏
- [ ] 权限检查已添加
- [ ] 数据权限已配置
- [ ] 多租户配置正确
- [ ] 验证注解完整
- [ ] 事务注解正确
- [ ] 本地测试通过

### PR 前检查

- [ ] Commit 符合规范
- [ ] PR 大小合理 (<3000 行)
- [ ] PR 描述完整
- [ ] 关联 Issue
- [ ] CI 测试通过
- [ ] 代码审查通过

## 框架特性速查表

| 特性 | Skill | 关键注解/配置 |
|------|-------|--------------|
| 多租户 | ry-multi-tenant | TenantEntity, TenantHelper |
| 数据权限 | ry-data-permission | @DataPermission, @DataColumn |
| 多数据源 | ry-multi-datasource | @DS, @DSTransactional |
| 权限认证 | ry-code-review | @SaCheckPermission, @SaCheckRole |
| 数据加密 | ry-code-review | @EncryptField |
| 数据脱敏 | ry-code-review | @Sensitive |
| 分页 | ry-code-review | PageQuery, PageQuery#build() |
| 代码生成 | ry-code-generator | 代码生成器 |
| Dubbo API | ry-dubbo-api | @RemoteService |
| 自定义登录 | ry-custom-auth | IAuthStrategy |
| Excel | ry-code-review | @ExcelProperty, @ExcelDictFormat |
| 缓存 | ry-code-review | @Cacheable, @CacheEvict |

## 常见问题

### Q1: 如何判断是否需要多租户？

**A**: 如果你的业务是 SaaS 多企业平台，企业之间数据需要完全隔离，则需要多租户。

### Q2: 如何判断是否需要数据权限？

**A**: 如果需要根据部门、角色等条件过滤数据访问，则需要数据权限。

### Q3: 如何选择事务注解？

**A**:
- 单数据源: @Transactional
- 本地多数据源: @DSTransactional
- 分布式事务: @GlobalTransactional

### Q4: 代码生成后需要修改什么？

**A**:
- Entity: 继承 TenantEntity 或 BaseEntity
- Bo: 添加验证注解
- Vo: 添加格式化、脱敏注解
- Mapper: 添加 @DataPermission
- Controller: 添加权限检查

### Q5: 如何调试多租户问题？

**A**:
```java
// 查看当前租户ID
String tenantId = LoginHelper.getTenantId();

// 忽略租户查看所有数据
TenantHelper.ignore(() -> {
    // 查询逻辑
});
```

## 工作流状态检查

```bash
# 检查当前分支
git branch --show-current

# 检查未提交的更改
git status

# 检查最近的 commit
git log -3 --oneline

# 检查 PR 状态
gh pr list --head $(git branch --show-current)

# 检查租户ID
LoginHelper.getTenantId()

# 检查当前用户
LoginHelper.getLoginUser()
```

## 最佳实践

### 1. 分支管理

- ✅ 每个功能一个分支
- ✅ 分支命名规范
- ✅ 完成后及时删除分支

### 2. 代码提交

- ✅ 小而频繁的提交
- ✅ 清晰的提交信息
- ✅ 遵循 Conventional Commits

### 3. PR 管理

- ✅ PR 大小合理
- ✅ PR 描述完整
- ✅ 及时响应审查意见

### 4. 代码质量

- ✅ 遵循框架规范
- ✅ 添加完整注释
- ✅ 编写单元测试
- ✅ 注意安全加密

## 前端同步

当后端 API 变更时，前端需要同步更新。请参考前端项目中的同步指南：

**前端同步指南**:
- `dragonboat-frontend/.claude/skills/ry-frontend-sync.md` - 后端变更时前端同步
- `dragonboat-frontend/.claude/skills/ry-frontend-workflow.md` - 前端开发工作流

### 后端 API 变更时的前端同步清单

| 后端变更 | 前端同步操作 |
|---------|-------------|
| 新增 CRUD 功能 | 创建 API types、API 方法、页面组件 |
| 修改接口路径 | 更新 API 调用路径 |
| 修改请求参数 | 更新 API 方法和调用代码 |
| 修改返回结构 | 更新 types 定义和组件代码 |
| 删除接口 | 移除相关 API 代码 |
| 新增权限标识 | 前端无需变更（后端控制） |
| 字段脱敏 | 前端无需变更（后端已处理） |

### 前后端分支对应

| 任务类型 | 后端分支 | 前端分支 |
|---------|---------|---------|
| 新功能 | feat/DB-101 | feat/FE-101 |
| Bug 修复 | fix/DB-205 | fix/FE-205 |
| 重构 | refactor/DB-301 | refactor/FE-301 |

### PR 关联

创建后端 PR 时，在描述中添加前端同步需求：

```markdown
## 前端同步任务
- [ ] 更新 API types 定义
- [ ] 更新 API 方法
- [ ] 更新页面组件
- [ ] 联调测试验证
```

创建前端 PR 时，在描述中注明依赖关系：

```markdown
## 依赖说明
- 依赖后端 PR: dragonboat-backend#XXX
- 依赖后端版本: v2.x.x
```

## 集成说明

此 skill 与以下组件集成:

**Skills (开发)**:
- ry-branch-management - 分支管理
- ry-multi-tenant - 多租户开发
- ry-data-permission - 数据权限
- ry-code-generator - 代码生成
- ry-crud - CRUD 开发
- ry-dubbo-api - Dubbo API
- ry-module-create - 新模块创建
- ry-multi-datasource - 多数据源
- ry-custom-auth - 自定义登录

**Skills (审查)**:
- ry-code-review - 代码审查
- ry-commit-standards - Commit 规范

**Hooks**:
- preToolUse.cjs - Git 操作验证
- postToolUse.cjs - 自动触发审查
- stop.cjs - 会话总结

**GitHub Actions**:
- pr-validate.yml - PR 验证
- ci-test.yml - CI 测试
- auto-code-review.yml - 自动审查

**Tools**:
- commit-commands:commit - 提交代码
- create-pr - 创建 PR
- code-review - 审查 PR

## 配置文件

相关配置位于:

- `.claude/skills/` - 所有 skills
- `.claude/hooks/` - 自动化 hooks
- `.github/workflows/` - CI/CD workflows
- `application-common.yml` - 框架通用配置
- `application.yml` - 服务配置
- `bootstrap.yml` - Nacos 配置

## 版本历史

- v2.0.0 (2026-01-17) - 重写，基于 RuoYi-Cloud-Plus 框架特性
- v1.0.0 (2026-01-17) - 初始版本
