# RuoYi-Cloud-Plus CRUD Development

适用于基于 RuoYi-Cloud-Plus 框架的标准 CRUD 功能开发，遵循项目的 DDD 分层架构和代码规范。

## 项目架构概述

RuoYi-Cloud-Plus 是一个基于 Spring Cloud + Dubbo 的微服务架构项目，使用 DDD（领域驱动设计）分层模式：

```
ruoyi-modules/{module}/
├── controller/          # 控制层 - 处理 HTTP 请求
├── service/            # 业务层 - 核心业务逻辑
│   ├── I{Xxx}Service.java
│   └── impl/{Xxx}ServiceImpl.java
├── mapper/             # 数据层 - MyBatis-Plus 数据访问
└── domain/             # 领域层 - 领域模型
    ├── {Xxx}.java        # 实体（数据库映射）
    ├── bo/{Xxx}Bo.java   # 业务对象（输入）
    ├── vo/{Xxx}Vo.java   # 视图对象（输出）
    └── convert/          # 对象转换器
```

## 命名规范

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 数据库表 | `sys_{entity}` | `sys_user` |
| 实体类 | `Sys{Entity}` | `SysUser` |
| 业务对象 | `{Entity}Bo` | `SysUserBo` |
| 视图对象 | `{Entity}Vo` | `SysUserVo` |
| Mapper | `{Entity}Mapper` | `SysUserMapper` |
| Service | `I{Entity}Service` | `ISysUserService` |
| Controller | `{Entity}Controller` | `SysUserController` |

## 代码模板

### 1. Entity（实体类）

**路径**: `domain/{Entity}.java`

```java
package org.dromara.{module}.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * {entityComment} {table_name}
 *
 * @author {author}
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("{table_name}")
public class {Entity} extends BaseEntity {

    /**
     * {pkComment}
     */
    @TableId(value = "{pk_column}")
    private {pkType} {pkField};

    /**
     * {fieldComment}
     */
    private String {field};

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;
}
```

**注意事项**:
- 如果需要多租户支持，继承 `TenantEntity` 而非 `BaseEntity`
- 使用 `@TableField` 配置字段策略（如密码字段）
- 使用 `@TableLogic` 标记逻辑删除字段

### 2. Bo（业务对象）

**路径**: `domain/bo/{Entity}Bo.java`

```java
package org.dromara.{module}.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.core.xss.Xss;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.{module}.domain.{Entity};

/**
 * {entityComment}业务对象 {table_name}
 *
 * @author {author}
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = {Entity}.class, reverseConvertGenerate = false)
public class {Entity}Bo extends BaseEntity {

    /**
     * {pkComment}
     */
    private {pkType} {pkField};

    /**
     * {fieldComment}
     */
    @Xss(message = "{field}不能包含脚本字符")
    @NotBlank(message = "{field}不能为空")
    @Size(min = 0, max = 50, message = "{field}长度不能超过{max}个字符")
    private String {field};

    /**
     * 业务扩展字段（不映射到数据库）
     */
    private String[] extraFields;
}
```

### 3. Vo（视图对象）

**路径**: `domain/vo/{Entity}Vo.java`

```java
package org.dromara.{module}.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.sensitive.annotation.Sensitive;
import org.dromara.common.sensitive.core.SensitiveStrategy;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.{module}.domain.{Entity};

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * {entityComment}视图对象 {table_name}
 *
 * @author {author}
 */
@Data
@AutoMapper(target = {Entity}.class)
public class {Entity}Vo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * {pkComment}
     */
    private {pkType} {pkField};

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * {fieldComment}
     */
    @Sensitive(strategy = SensitiveStrategy.PHONE, perms = "{module}:{entity}:edit")
    private String {field};

    /**
     * 字段翻译示例
     */
    @Translation(type = TransConstant.DEPT_ID_TO_NAME, mapper = "deptId")
    private String deptName;

    /**
     * 创建时间
     */
    private Date createTime;
}
```

### 4. Mapper（数据访问）

**路径**: `mapper/{Entity}Mapper.java`

```java
package org.dromara.{module}.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.{module}.domain.{Entity};
import org.dromara.{module}.domain.vo.{Entity}Vo;

import java.util.List;

/**
 * {entityComment} 数据层
 *
 * @author {author}
 */
public interface {Entity}Mapper extends BaseMapperPlus<{Entity}, {Entity}Vo> {

    /**
     * 分页查询{entityComment}列表，并进行数据权限控制
     *
     * @param page         分页参数
     * @param queryWrapper 查询条件
     * @return 分页的{entityComment}信息
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "create_by")
    })
    default Page<{Entity}Vo> selectPageList(Page<{Entity}> page, Wrapper<{Entity}> queryWrapper) {
        return this.selectVoPage(page, queryWrapper);
    }

    /**
     * 查询{entityComment}列表，并进行数据权限控制
     *
     * @param queryWrapper 查询条件
     * @return {entityComment}信息集合
     */
    @DataPermission({...})
    default List<{Entity}Vo> selectList(Wrapper<{Entity}> queryWrapper) {
        return this.selectVoList(queryWrapper);
    }
}
```

### 5. Service（业务层）

**接口**: `service/I{Entity}Service.java`

```java
package org.dromara.{module}.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.{module}.domain.bo.{Entity}Bo;
import org.dromara.{module}.domain.vo.{Entity}Vo;

import java.util.List;

/**
 * {entityComment} 业务层
 *
 * @author {author}
 */
public interface I{Entity}Service {

    /**
     * 分页查询{entityComment}列表
     *
     * @param bo        业务对象
     * @param pageQuery 分页参数
     * @return {entityComment}信息集合
     */
    TableDataInfo<{Entity}Vo> selectPageList({Entity}Bo bo, PageQuery pageQuery);

    /**
     * 根据{pkComment}查询{entityComment}
     *
     * @param {pkField} {pkComment}
     * @return {entityComment}对象信息
     */
    {Entity}Vo selectById({pkType} {pkField});

    /**
     * 校验{name}是否唯一
     *
     * @param bo 业务对象
     * @return 结果
     */
    boolean checkUnique({Entity}Bo bo);

    /**
     * 新增{entityComment}
     *
     * @param bo 业务对象
     * @return 结果
     */
    int insert({Entity}Bo bo);

    /**
     * 修改{entityComment}
     *
     * @param bo 业务对象
     * @return 结果
     */
    int update({Entity}Bo bo);

    /**
     * 批量删除{entityComment}
     *
     * @param {pkField}s 需要删除的{pkComment}
     * @return 结果
     */
    int deleteByIds({pkType}[] {pkField}s);
}
```

**实现类**: `service/impl/{Entity}ServiceImpl.java`

```java
package org.dromara.{module}.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.{module}.domain.{Entity};
import org.dromara.{module}.domain.bo.{Entity}Bo;
import org.dromara.{module}.domain.vo.{Entity}Vo;
import org.dromara.{module}.mapper.{Entity}Mapper;
import org.dromara.{module}.service.I{Entity}Service;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * {entityComment} 业务层处理
 *
 * @author {author}
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class {Entity}ServiceImpl implements I{Entity}Service {

    private final {Entity}Mapper baseMapper;

    @Override
    public TableDataInfo<{Entity}Vo> selectPageList({Entity}Bo bo, PageQuery pageQuery) {
        Page<{Entity}Vo> page = baseMapper.selectPageList(
            pageQuery.build(),
            this.buildQueryWrapper(bo)
        );
        return TableDataInfo.build(page);
    }

    private Wrapper<{Entity}> buildQueryWrapper({Entity}Bo bo) {
        LambdaQueryWrapper<{Entity}> wrapper = Wrappers.lambdaQuery();
        wrapper.eq({Entity}::getDelFlag, "0")
            .like(StringUtils.isNotBlank(bo.getName()), {Entity}::getName, bo.getName())
            .eq(ObjectUtil.isNotNull(bo.getId()), {Entity}::getId, bo.getId());
        return wrapper;
    }

    @Override
    public {Entity}Vo selectById({pkType} {pkField}) {
        return baseMapper.selectVoById({pkField});
    }

    @Override
    public boolean checkUnique({Entity}Bo bo) {
        {pkType} {pkField} = ObjectUtil.isNull(bo.getId()) ? -1L : bo.getId();
        {Entity} entity = baseMapper.selectOne(new LambdaQueryWrapper<{Entity}>()
            .eq({Entity}::getName, bo.getName())
            .ne({Entity}::getId, {pkField}));
        return ObjectUtil.isNull(entity);
    }

    @Override
    public int insert({Entity}Bo bo) {
        {Entity} entity = MapstructUtils.convert(bo, {Entity}.class);
        return baseMapper.insert(entity);
    }

    @Override
    public int update({Entity}Bo bo) {
        {Entity} entity = MapstructUtils.convert(bo, {Entity}.class);
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteByIds({pkType}[] {pkField}s) {
        return baseMapper.deleteByIds(Arrays.asList({pkField}s));
    }
}
```

### 6. Controller（控制器）

**路径**: `controller/{Entity}Controller.java`

```java
package org.dromara.{module}.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.{module}.domain.bo.{Entity}Bo;
import org.dromara.{module}.domain.vo.{Entity}Vo;
import org.dromara.{module}.service.I{Entity}Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * {entityComment}
 *
 * @author {author}
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/{entity}")
public class {Entity}Controller extends BaseController {

    private final I{Entity}Service {entity}Service;

    /**
     * 查询{entityComment}列表
     */
    @SaCheckPermission("{module}:{entity}:list")
    @GetMapping("/list")
    public TableDataInfo<{Entity}Vo> list({Entity}Bo bo, PageQuery pageQuery) {
        return {entity}Service.selectPageList(bo, pageQuery);
    }

    /**
     * 根据{pkComment}查询{entityComment}
     *
     * @param {pkField} {pkComment}
     */
    @SaCheckPermission("{module}:{entity}:query")
    @GetMapping("/{pkField}")
    public R<{Entity}Vo> getInfo(@PathVariable {pkType} {pkField}) {
        return R.ok({entity}Service.selectById({pkField}));
    }

    /**
     * 新增{entityComment}
     */
    @SaCheckPermission("{module}:{entity}:add")
    @Log(title = "{entityComment}", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Valid @RequestBody {Entity}Bo bo) {
        return toAjax({entity}Service.insert(bo));
    }

    /**
     * 修改{entityComment}
     */
    @SaCheckPermission("{module}:{entity}:edit")
    @Log(title = "{entityComment}", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Valid @RequestBody {Entity}Bo bo) {
        return toAjax({entity}Service.update(bo));
    }

    /**
     * 删除{entityComment}
     */
    @SaCheckPermission("{module}:{entity}:remove")
    @Log(title = "{entityComment}", businessType = BusinessType.DELETE)
    @DeleteMapping("/{pkField}s")
    public R<Void> remove(@PathVariable {pkType}[] {pkField}s) {
        return toAjax({entity}Service.deleteByIds({pkField}s));
    }
}
```

## 常用注解说明

### Controller 层注解
- `@SaCheckPermission("{module}:{entity}:list")` - 权限检查
- `@Log(title = "", businessType = BusinessType.INSERT)` - 操作日志
- `@RepeatSubmit()` - 防重复提交
- `@Validated` - 启用参数校验

### Service 层注解
- `@Cacheable(cacheNames = CacheNames.SYS_XXX, key = "#id")` - 缓存读取
- `@CacheEvict(cacheNames = CacheNames.SYS_XXX, key = "#bo.id")` - 清除缓存
- `@Transactional(rollbackFor = Exception.class)` - 事务管理

### Domain 层注解
- `@AutoMapper(target = Entity.class)` - MapStruct 自动映射
- `@Sensitive(strategy = SensitiveStrategy.EMAIL)` - 数据脱敏
- `@Translation(type = TransConstant.DEPT_ID_TO_NAME)` - 字段翻译

### Mapper 层注解
- `@DataPermission` - 数据权限控制

## 依赖模块

开发新模块时需要依赖的 common 模块：

```xml
<!-- MyBatis-Plus -->
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-common-mybatis</artifactId>
</dependency>
<!-- Web -->
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-common-web</artifactId>
</dependency>
<!-- Dubbo -->
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-common-dubbo</artifactId>
</dependency>
<!-- 多租户 -->
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-common-tenant</artifactId>
</dependency>
<!-- Excel -->
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-common-excel</artifactId>
</dependency>
```

## 开发检查清单

- [ ] Entity 继承正确的基类（BaseEntity 或 TenantEntity）
- [ ] Bo 添加 `@AutoMapper` 注解
- [ ] Vo 添加 `@AutoMapper` 注解
- [ ] Controller 方法添加权限检查 `@SaCheckPermission`
- [ ] Controller 方法添加操作日志 `@Log`
- [ ] Controller 修改/删除方法添加 `@RepeatSubmit`
- [ ] Service 方法添加缓存注解（如需要）
- [ ] Mapper 方法添加数据权限注解（如需要）
- [ ] Bo 添加校验注解（@NotNull, @NotBlank 等）
- [ ] Vo 敏感字段添加脱敏注解
