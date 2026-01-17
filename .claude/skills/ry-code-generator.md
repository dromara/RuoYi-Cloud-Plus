# RuoYi-Cloud-Plus Code Generator

Dragonboat-Backend 基于 RuoYi-Cloud-Plus 框架的代码生成器使用指南。

## 触发词

- "代码生成"
- "code generator"
- "生成CRUD"
- "生成代码"
- "代码生成器"
- "generate code"

## 功能介绍

### 核心特性

- **100+ 数据库支持** - MySQL, Oracle, PostgreSQL, SQLServer, 达梦, 金仓等
- **多数据源切换** - 动态切换不同数据库进行代码生成
- **一键生成代码** - Entity, Bo, Vo, Mapper, Service, Controller, 前端页面
- **自动适配规范** - 遵循 MyBatis-Plus 和 SpringDoc 规范
- **智能字段配置** - 插入、编辑、查询、列表、必填、显示类型
- **树表支持** - 自动生成树表结构代码
- **代码预览** - 生成前预览代码结构
- **字段同步** - 与数据库实时同步字段

### 生成的代码结构

```
生成前                    →  生成后
database table            →  Entity (实体类)
                          →  Bo (业务对象)
                          →  Vo (视图对象)
                          →  Convert (对象转换器)
                          →  Mapper (MyBatis-Plus 接口)
                          →  Mapper.xml (自定义 SQL 映射)
                          →  Service (业务接口)
                          →  ServiceImpl (业务实现)
                          →  Controller (控制器)
                          →  HTML (前端页面)
                          →  API.js (前端 API)
```

## 使用流程

### Step 1: 切换数据源

**位置**: 系统工具 → 代码生成 → 数据源配置

```yaml
# 支持的数据源类型
- MySQL 5.7/8.0
- Oracle 12c+
- PostgreSQL 13/14/15
- SQL Server
- 达梦
- 金仓
- 以及其他 100+ 数据库
```

**操作步骤**:
1. 填写数据源名称
2. 点击搜索按钮切换数据源
3. 系统会自动连接并加载数据库信息

### Step 2: 导入数据表

**位置**: 系统工具 → 代码生成 → 导入按钮

**操作步骤**:
1. 点击导入按钮
2. 系统加载当前数据源所有表
3. 勾选需要生成代码的表
4. 点击确定导入

### Step 3: 编辑表生成信息

**位置**: 系统工具 → 代码生成 → 编辑按钮

#### 基本信息配置

```
表名称:        your_table_name
表描述:        业务表描述
模块名称:      system (对应服务模块)
业务名称:      yourBusiness (生成类名前缀)
功能名称:      业务功能
作者:          你的名字
包路径:        org.dromara.system
生成路径:      /ruoyi-modules/system
```

#### 字段配置

| 字段 | 说明 | 生成影响 |
|------|------|----------|
| 插入 | ✅/❌ | Bo 类 + 前端添加页面 |
| 编辑 | ✅/❌ | Bo 类 + 前端编辑页面 |
| 列表 | ✅/❌ | Vo 类 + 前端列表展示 |
| 查询 | ✅/❌ | 前端搜索框 + 后端查询条件 |
| 查询方式 | =, !=, >, <, BETWEEN, LIKE | 查询条件类型 |
| 必填 | ✅/❌ | Bo 校验注解 + 页面必填 |
| 显示类型 | text, input, select, date, datetime... | 页面组件类型 |
| 字典类型 | 字典类型编码 | 字典关联 |

**查询方式详解**:
- `=` - 等于查询（默认）
- `!=` - 不等于查询
- `>` - 大于查询
- `<` - 小于查询
- `BETWEEN` - 范围查询
- `LIKE` - 模糊查询

**显示类型详解**:
- `文本框` - 普通文本输入
- `文本域` - 多行文本输入
- `下拉框` - 固定选项选择
- `单选框` - 单选
- `复选框` - 多选
- `日期` - 日期选择
- `日期时间` - 日期时间选择
- `图片上传` - 图片上传
- `文件上传` - 文件上传
- `富文本` - 富文本编辑器

#### 树表配置

**适用场景**: 有层级关系的数据（部门、菜单、分类等）

**配置项**:
```
生成模板:  树表
树编码字段: 树节点的编码字段（如 id）
树父编码字段: 树节点的父编码字段（如 parent_id）
树名称字段: 树节点显示的名称字段（如 name）
```

### Step 4: 预览代码

**位置**: 编辑页面 → 预览按钮

**功能**:
- 查看生成的代码结构
- 检查字段配置是否正确
- 确认代码规范是否符合要求

**预览内容包括**:
- Entity.java
- Bo.java
- Vo.java
- Convert.java
- Mapper.java
- Mapper.xml
- Service.java
- ServiceImpl.java
- Controller.java
- index.vue
- api.js

### Step 5: 生成代码

**方式一**: 在线编辑
1. 点击编辑按钮
2. 修改代码
3. 复制到项目中

**方式二**: 下载压缩包
1. 点击生成代码按钮
2. 下载 ZIP 文件
3. 解压到项目目录

**方式三**: 直接生成（推荐用于开发环境）
1. 配置好生成路径
2. 点击生成代码
3. 代码直接生成到项目

### Step 6: 字段同步

**适用场景**: 数据库表结构发生变化

**操作步骤**:
1. 点击同步按钮
2. 系统自动与数据库同步字段
3. 更新配置信息

> ⚠️ **注意**: 同步会覆盖现有配置，请谨慎操作

## 字段配置详细说明

### 1. 插入/编辑配置

```
插入: ✅  → 在 Bo 中生成字段
        → 在前端添加页面生成对应输入框
编辑: ✅  → 在 Bo 中生成字段
        → 在前端编辑页面生成对应输入框
```

**Bo 示例**:
```java
public class YourBo extends PageQuery {
    @NotBlank(message = "名称不能为空")
    private String name;
}
```

### 2. 列表配置

```
列表: ✅  → 在 Vo 中生成字段
        → 在前端列表页面展示该列
```

**Vo 示例**:
```java
public class YourVo {
    @ExcelProperty(value = "名称")
    private String name;
}
```

### 3. 查询配置

```
查询: ✅  → 在前端生成搜索框
        → 在后端生成查询条件
查询方式: LIKE  → WHERE name LIKE CONCAT('%', #{name}, '%')
查询方式: =     → WHERE name = #{name}
```

### 4. 必填配置

```
必填: ✅  → 在 Bo 中添加 @NotBlank 注解
        → 在前端添加 required 属性
```

**校验注解示例**:
```java
@NotNull(message = "ID不能为空")
private Long id;

@NotBlank(message = "名称不能为空")
private String name;

@Email(message = "邮箱格式错误")
private String email;

@Size(min = 2, max = 20, message = "名称长度2-20字符")
private String name;
```

### 5. 字典类型配置

```
字典类型: sys_normal_disable
        → 前端自动生成下拉框
        → 自动关联字典数据
        → 自动翻译显示
```

**字典配置示例**:
```java
@DictFormat(type = DictType.SYS_NORMAL_DISABLE)
private String status;
```

## 树表生成

### 配置示例

```
表名称: sys_dept
生成模板: 树表
树编码字段: dept_id
树父编码字段: parent_id
树名称字段: dept_name
```

### 生成的代码特性

- 自动处理树形结构
- 自动生成父子关系查询
- 前端自动使用树形组件
- 支持拖拽排序

## 主子表说明

> ⚠️ **框架不支持也不推荐使用主子表**

**原因**:
1. 实际业务场景大多是复杂的多表关联
2. 主子表容易产生笛卡尔积
3. 数据错乱风险高
4. SQL 调优困难

**推荐做法**:
- 按单表生成代码
- 自行编写业务逻辑处理关联关系
- 分解大连接查询为多个单表查询

## 生成的代码规范

### 1. 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 表名 | `sys_` + 业务 | `sys_user_info` |
| Entity | `Sys` + 业务 | `SysUserInfo` |
| Bo | 业务 + `Bo` | `SysUserInfoBo` |
| Vo | 业务 + `Vo` | `SysUserInfoVo` |
| Mapper | 业务 + `Mapper` | `SysUserInfoMapper` |
| Service | `I` + 业务 + `Service` | `ISysUserInfoService` |
| Controller | 业务 + `Controller` | `SysUserInfoController` |

### 2. 注释规范

生成的代码会自动添加 Javadoc 注释，用于 SpringDoc 文档生成。

```java
/**
 * 业务表 one_sys_user
 *
 * @author dragonboat
 * @date 2026-01-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("one_sys_user")
public class OneSysUser extends TenantEntity {
    /**
     * 用户ID
     */
    @TableId(value = "user_id")
    private Long userId;

    /**
     * 用户账号
     */
    private String userName;
}
```

### 3. MyBatis-Plus 规范

生成的代码完全遵循 MyBatis-Plus 规范：

```java
// Mapper 继承 BaseMapperPlus
public interface SysUserMapper extends BaseMapperPlus<SysUser, SysUserVo> {
    // 自定义方法...
}

// Service 继承 ServiceImpl
public class SysUserServiceImpl
    extends ServiceImpl<SysUserMapper, SysUser>
    implements ISysUserService {
    // 业务方法...
}
```

### 4. SpringDoc 规范

生成的 Controller 自动包含 SpringDoc 注解：

```java
@Tag(name = "用户管理", description = "用户管理接口")
@RestController
@RequestMapping("/system/user")
public class SysUserController {

    @Operation(summary = "查询用户列表")
    @GetMapping("/list")
    public TableDataInfo<SysUserVo> list(SysUserBo bo) {
        // ...
    }
}
```

## 代码生成后操作

### 1. 检查生成的代码

```bash
# 检查文件是否生成完整
ls -la ruoyi-modules/system/domain/
ls -la ruoyi-modules/system/mapper/
ls -la ruoyi-modules/system/service/
ls -la ruoyi-modules/system/controller/
```

### 2. 调整业务逻辑

根据实际需求调整 Service 层的业务逻辑。

### 3. 添加权限注解

```java
@SaCheckPermission("system:your:list")
@GetMapping("/list")
public TableDataInfo<YourVo> list(YourBo bo) {
    // ...
}
```

### 4. 添加数据权限

```java
@DataPermission({
    @DataColumn(key = "deptName", value = "dept_id")
})
List<YourVo> selectList(YourBo bo);
```

### 5. 测试接口

```bash
# 测试列表接口
curl http://localhost:8080/system/your/list

# 测试详情接口
curl http://localhost:8080/system/your/{id}

# 测试新增接口
curl -X POST http://localhost:8080/system/your/add \
  -H "Content-Type: application/json" \
  -d '{"name":"test"}'
```

## 多数据源代码生成

### 配置数据源

**位置**: 代码生成 → 数据源配置

```yaml
# 主数据源
master:
  driver-class-name: com.mysql.cj.jdbc.Driver
  url: jdbc:mysql://localhost:3306/ry-vue3
  username: root
  password: root

# 从数据源
slave:
  driver-class-name: com.mysql.cj.jdbc.Driver
  url: jdbc:mysql://localhost:3306/ry-vue3-slave
  username: root
  password: root
```

### 生成代码

切换到对应数据源后，按照正常流程生成代码。

## 最佳实践

### 1. 表设计规范

```sql
CREATE TABLE `your_table` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`)
) COMMENT='业务表';
```

### 2. 字段命名规范

```sql
-- ✅ 推荐
user_name       → userName
dept_id         → deptId
create_time     → createTime
is_deleted      → isDeleted

-- ❌ 不推荐
user_name       → user_name (不符合 Java 命名)
deptID          → deptId (不统一)
createtime      → createTime (不清晰)
```

### 3. 生成前检查清单

- [ ] 表字段命名规范
- [ ] 必备字段齐全
- [ ] 主键类型正确
- [ ] 租户表包含 tenant_id
- [ ] 字段注释完整

### 4. 生成后检查清单

- [ ] 代码生成完整
- [ ] 包名路径正确
- [ ] 类名符合规范
- [ ] 注释清晰完整
- [ ] 接口测试通过

## 常见问题

### 问题 1: 生成的代码无法编译

**原因**: 缺少依赖或包名错误

**解决方案**:
```xml
<!-- 检查 pom.xml 依赖 -->
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-common-mybatis</artifactId>
</dependency>
```

### 问题 2: 前端页面无法显示

**原因**: 生成路径错误或未重启服务

**解决方案**:
1. 检查前端文件生成路径
2. 重新编译前端项目
3. 刷新浏览器缓存

### 问题 3: 字典翻译不生效

**原因**: 字典类型配置错误

**解决方案**:
1. 检查字典类型是否存在
2. 确认字典类型编码正确
3. 刷新字典缓存

## 参考资源

- [RuoYi-Cloud-Plus 代码生成文档](https://plus-doc.dromara.org/ruoyi-cloud-plus/framework/basic/code_generate.html)
- [MyBatis-Plus 官方文档](https://baomidou.com/)
- [SpringDoc 官方文档](https://springdoc.org/)

## 版本历史

- v1.0.0 (2026-01-17) - 初始版本，基于 RuoYi-Cloud-Plus 2.5.X
