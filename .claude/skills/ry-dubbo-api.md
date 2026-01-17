# RuoYi-Cloud-Plus Dubbo RPC API 开发

适用于 RuoYi-Cloud-Plus 项目中 Dubbo RPC 远程服务的开发，包括 API 接口定义和 Dubbo 服务实现。

## 架构说明

RuoYi-Cloud-Plus 使用 **Dubbo RPC** 作为微服务间通信的主要方式：

```
┌─────────────────┐         Dubbo RPC          ┌─────────────────┐
│   Consumer      │ ──────────────────────────> │   Provider      │
│  (调用方服务)    │                             │  (服务提供方)    │
└─────────────────┘                             └─────────────────┘
                                                     │
                                                     │
┌─────────────────┐                             ┌─────────────────┐
│  ruoyi-api/     │ <──────────────────────────  │   dubbo/        │
│  API接口定义     │      implements              │   服务实现       │
└─────────────────┘                             └─────────────────┘
```

## 模块结构

### API 模块 (ruoyi-api)

定义远程服务接口和 DTO：

```
ruoyi-api/ruoyi-api-{module}/
├── src/main/java/org/dromara/{module}/api/
│   ├── Remote{Xxx}Service.java           # 远程服务接口
│   ├── domain/bo/
│   │   └── Remote{Xxx}Bo.java            # 远程业务对象
│   ├── domain/vo/
│   │   └── Remote{Xxx}Vo.java            # 远程视图对象
│   └── model/
│       ├── LoginUser.java                # 登录用户模型
│       └── {Xxx}DTO.java                 # 数据传输对象
```

### 服务实现模块 (ruoyi-modules)

实现 Dubbo 服务：

```
ruoyi-modules/ruoyi-{module}/
└── src/main/java/org/dromara/{module}/
    └── dubbo/
        └── Remote{Xxx}ServiceImpl.java    # Dubbo服务实现
```

## 代码模板

### 1. 远程服务接口

**路径**: `ruoyi-api/ruoyi-api-{module}/src/main/java/org/dromara/{module}/api/Remote{Xxx}Service.java`

```java
package org.dromara.{module}.api;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.{module}.api.domain.bo.Remote{Xxx}Bo;
import org.dromara.{module}.api.domain.vo.Remote{Xxx}Vo;

import java.util.List;

/**
 * {xxxComment}服务
 *
 * @author {author}
 */
public interface Remote{Xxx}Service {

    /**
     * 根据{idComment}查询{xxxComment}信息
     *
     * @param id       {idComment}
     * @param tenantId 租户id
     * @return 结果
     */
    Remote{Xxx}Vo getInfo(Long id, String tenantId) throws ServiceException;

    /**
     * 根据{fieldComment}查询{xxxComment}信息
     *
     * @param {field}  {fieldComment}
     * @param tenantId 租户id
     * @return 结果
     */
    Remote{Xxx}Vo getInfoBy{Field}(String {field}, String tenantId) throws ServiceException;

    /**
     * 新增{xxxComment}信息
     *
     * @param bo 业务对象
     * @return 结果
     */
    Boolean add(Remote{Xxx}Bo bo) throws ServiceException;

    /**
     * 修改{xxxComment}信息
     *
     * @param bo 业务对象
     * @return 结果
     */
    Boolean update(Remote{Xxx}Bo bo) throws ServiceException;

    /**
     * 批量删除{xxxComment}
     *
     * @param ids {idComment}列表
     * @param tenantId 租户id
     * @return 结果
     */
    Boolean deleteByIds(List<Long> ids, String tenantId) throws ServiceException;

    /**
     * 根据{idComment}查询{xxxComment}名称
     *
     * @param id {idComment}
     * @return {xxxComment}名称
     */
    String getNameById(Long id);

    /**
     * 根据{idComment}列表查询{xxxComment}列表
     *
     * @param ids {idComment}列表
     * @return {xxxComment}列表
     */
    List<Remote{Xxx}Vo> listByIds(List<Long> ids);
}
```

### 2. 远程业务对象 (Remote Bo)

**路径**: `ruoyi-api/ruoyi-api-{module}/src/main/java/org/dromara/{module}/api/domain/bo/Remote{Xxx}Bo.java`

```java
package org.dromara.{module}.api.domain.bo;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * {xxxComment}远程业务对象
 *
 * @author {author}
 */
@Data
public class Remote{Xxx}Bo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * {idComment}
     */
    private Long id;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * {fieldComment}
     */
    @NotBlank(message = "{field}不能为空")
    @Size(max = 50, message = "{field}长度不能超过{max}个字符")
    private String {field};

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
```

### 3. 远程视图对象 (Remote Vo)

**路径**: `ruoyi-api/ruoyi-api-{module}/src/main/java/org/dromara/{module}/api/domain/vo/Remote{Xxx}Vo.java`

```java
package org.dromara.{module}.api.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * {xxxComment}远程视图对象
 *
 * @author {author}
 */
@Data
public class Remote{Xxx}Vo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * {idComment}
     */
    private Long id;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * {fieldComment}
     */
    private String {field};

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;
}
```

### 4. Dubbo 服务实现

**路径**: `ruoyi-modules/ruoyi-{module}/src/main/java/org/dromara/{module}/dubbo/Remote{Xxx}ServiceImpl.java`

```java
package org.dromara.{module}.dubbo;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.{module}.api.Remote{Xxx}Service;
import org.dromara.{module}.api.domain.bo.Remote{Xxx}Bo;
import org.dromara.{module}.api.domain.vo.Remote{Xxx}Vo;
import org.dromara.{module}.domain.{Xxx};
import org.dromara.{module}.domain.bo.{Xxx}Bo;
import org.dromara.{module}.domain.vo.{Xxx}Vo;
import org.dromara.{module}.mapper.{Xxx}Mapper;
import org.dromara.{module}.service.I{Xxx}Service;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * {xxxComment}服务
 *
 * @author {author}
 */
@RequiredArgsConstructor
@Service
@DubboService
public class Remote{Xxx}ServiceImpl implements Remote{Xxx}Service {

    private final I{Xxx}Service {xxx}Service;
    private final {Xxx}Mapper {xxx}Mapper;

    /**
     * 根据{idComment}查询{xxxComment}信息
     *
     * @param id       {idComment}
     * @param tenantId 租户id
     * @return 结果
     */
    @Override
    public Remote{Xxx}Vo getInfo(Long id, String tenantId) throws ServiceException {
        return TenantHelper.dynamic(tenantId, () -> {
            {Xxx}Vo vo = {xxx}Service.selectById(id);
            if (ObjectUtil.isNull(vo)) {
                throw new ServiceException("{xxxComment}不存在");
            }
            return MapstructUtils.convert(vo, Remote{Xxx}Vo.class);
        });
    }

    /**
     * 根据{fieldComment}查询{xxxComment}信息
     *
     * @param {field}  {fieldComment}
     * @param tenantId 租户id
     * @return 结果
     */
    @Override
    public Remote{Xxx}Vo getInfoBy{Field}(String {field}, String tenantId) throws ServiceException {
        return TenantHelper.dynamic(tenantId, () -> {
            {Xxx}Vo vo = {xxx}Mapper.selectVoOne(new LambdaQueryWrapper<{Xxx}>()
                .eq({Xxx}::getField, {field}));
            if (ObjectUtil.isNull(vo)) {
                throw new ServiceException("{xxxComment}不存在");
            }
            return MapstructUtils.convert(vo, Remote{Xxx}Vo.class);
        });
    }

    /**
     * 新增{xxxComment}信息
     *
     * @param bo 业务对象
     * @return 结果
     */
    @Override
    public Boolean add(Remote{Xxx}Bo bo) throws ServiceException {
        {Xxx}Bo {xxx}Bo = MapstructUtils.convert(bo, {Xxx}Bo.class);
        return TenantHelper.dynamic(bo.getTenantId(), () -> {
            return {xxx}Service.insert({xxx}Bo) > 0;
        });
    }

    /**
     * 修改{xxxComment}信息
     *
     * @param bo 业务对象
     * @return 结果
     */
    @Override
    public Boolean update(Remote{Xxx}Bo bo) throws ServiceException {
        {Xxx}Bo {xxx}Bo = MapstructUtils.convert(bo, {Xxx}Bo.class);
        return TenantHelper.dynamic(bo.getTenantId(), () -> {
            return {xxx}Service.update({xxx}Bo) > 0;
        });
    }

    /**
     * 批量删除{xxxComment}
     *
     * @param ids      {idComment}列表
     * @param tenantId 租户id
     * @return 结果
     */
    @Override
    public Boolean deleteByIds(List<Long> ids, String tenantId) throws ServiceException {
        if (CollUtil.isEmpty(ids)) {
            return true;
        }
        return TenantHelper.dynamic(tenantId, () -> {
            return {xxx}Service.deleteByIds(ids.toArray(new Long[0])) > 0;
        });
    }

    /**
     * 根据{idComment}查询{xxxComment}名称
     *
     * @param id {idComment}
     * @return {xxxComment}名称
     */
    @Override
    public String getNameById(Long id) {
        {Xxx}Vo vo = {xxx}Service.selectById(id);
        return ObjectUtil.isNull(vo) ? null : vo.getName();
    }

    /**
     * 根据{idComment}列表查询{xxxComment}列表
     *
     * @param ids {idComment}列表
     * @return {xxxComment}列表
     */
    @Override
    public List<Remote{Xxx}Vo> listByIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return new ArrayList<>();
        }
        List<{Xxx}Vo> list = {xxx}Mapper.selectVoList(new LambdaQueryWrapper<{Xxx}>()
            .in({Xxx}::getId, ids));
        return MapstructUtils.convert(list, Remote{Xxx}Vo.class);
    }
}
```

### 5. 服务消费者调用

在消费者服务中注入并使用远程服务：

```java
package org.dromara.{consumer-module}.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.{module}.api.Remote{Xxx}Service;
import org.dromara.{module}.api.domain.vo.Remote{Xxx}Vo;
import org.springframework.stereotype.Service;

/**
 * 服务消费者示例
 */
@RequiredArgsConstructor
@Service
public class {Consumer}ServiceImpl {

    @DubboReference
    private Remote{Xxx}Service remote{Xxx}Service;

    public void doSomething() {
        // 调用远程服务
        String tenantId = LoginHelper.getTenantId();
        Remote{Xxx}Vo vo = remote{Xxx}Service.getInfo(1L, tenantId);

        // 批量查询
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        List<Remote{Xxx}Vo> list = remote{Xxx}Service.listByIds(ids);
    }
}
```

## 关键注解说明

### Dubbo 注解
- `@DubboService` - 标记 Dubbo 服务提供者
- `@DubboReference` - 注入 Dubbo 服务引用

### 多租户处理
- `TenantHelper.dynamic(tenantId, () -> {...})` - 在指定租户上下文中执行操作

### 异常处理
- 接口方法声明 `throws ServiceException`
- 实现中抛出具体的业务异常

## 最佳实践

### 1. 租户上下文处理

所有需要数据隔离的方法都应该使用 `TenantHelper.dynamic()`:

```java
@Override
public Remote{Xxx}Vo getInfo(Long id, String tenantId) {
    return TenantHelper.dynamic(tenantId, () -> {
        // 在指定租户上下文中执行
        return {xxx}Service.selectById(id);
    });
}
```

### 2. 空值处理

在查询方法中进行空值检查：

```java
@Override
public Remote{Xxx}Vo getInfo(Long id, String tenantId) {
    return TenantHelper.dynamic(tenantId, () -> {
        {Xxx}Vo vo = {xxx}Service.selectById(id);
        if (ObjectUtil.isNull(vo)) {
            throw new ServiceException("{xxxComment}不存在，ID: " + id);
        }
        return MapstructUtils.convert(vo, Remote{Xxx}Vo.class);
    });
}
```

### 3. 批量操作空列表处理

```java
@Override
public List<Remote{Xxx}Vo> listByIds(List<Long> ids) {
    if (CollUtil.isEmpty(ids)) {
        return new ArrayList<>();
    }
    // 执行查询
}
```

### 4. 对象转换

使用 `MapstructUtils.convert()` 进行对象转换：

```java
// Remote Bo -> Module Bo
{Xxx}Bo {xxx}Bo = MapstructUtils.convert(remoteBo, {Xxx}Bo.class);

// Module Vo -> Remote Vo
Remote{Xxx}Vo remoteVo = MapstructUtils.convert(moduleVo, Remote{Xxx}Vo.class);
```

## API 模块依赖配置

在 `ruoyi-api/ruoyi-api-{module}/pom.xml` 中：

```xml
<dependencies>
    <!-- 只依赖 core，不依赖其他模块 -->
    <dependency>
        <groupId>org.dromara</groupId>
        <artifactId>ruoyi-common-core</artifactId>
    </dependency>
</dependencies>
```

## 服务实现模块依赖配置

在 `ruoyi-modules/ruoyi-{module}/pom.xml` 中：

```xml
<dependencies>
    <!-- 依赖对应的 API 模块 -->
    <dependency>
        <groupId>org.dromara</groupId>
        <artifactId>ruoyi-api-{module}</artifactId>
    </dependency>
    <!-- Dubbo 支持 -->
    <dependency>
        <groupId>org.dromara</groupId>
        <artifactId>ruoyi-common-dubbo</artifactId>
    </dependency>
    <!-- 多租户支持 -->
    <dependency>
        <groupId>org.dromara</groupId>
        <artifactId>ruoyi-common-tenant</artifactId>
    </dependency>
</dependencies>
```

## 开发检查清单

- [ ] API 接口方法添加 `throws ServiceException`
- [ ] 远程对象实现 `Serializable` 接口
- [ ] 远程对象添加 `@Serial` 注解的 `serialVersionUID`
- [ ] 服务实现类添加 `@DubboService` 注解
- [ ] 服务消费者使用 `@DubboReference` 注入
- [ ] 涉及多租户的方法使用 `TenantHelper.dynamic()`
- [ ] 空值检查，返回或抛出适当的异常
- [ ] 批量操作检查空列表
- [ ] 对象转换使用 `MapstructUtils.convert()`
- [ ] Remote Bo/Vo 与 Module Bo/Vo 字段对应
