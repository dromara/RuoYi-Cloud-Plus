# RuoYi-Cloud-Plus Custom Login Development

Dragonboat-Backend 基于 RuoYi-Cloud-Plus 框架的自定义登录开发指南。

## 触发词

- "自定义登录"
- "custom login"
- "新增登录方式"
- "登录策略"
- "认证方式"
- "authentication"

## 登录系统概述

### 核心设计

框架采用 **Sa-Token** 控制权限，支持多种登录方式：

- **密码登录** - 用户名+密码
- **短信登录** - 手机号+验证码
- **邮件登录** - 邮箱+验证码
- **小程序登录** - 微信小程序授权
- **第三方登录** - Gitee、GitHub、钉钉等

### 关键特性

> ⚠️ **重要**: 系统全局统一只有一个登录接口 (`/auth/login`)，只需增加不同的鉴权方式即可

**不限制用户数据来源**，只需构建 `LoginUser` 对象即可完成登录：
- 同表不同类型
- 不同表
- 同表 + 扩展表

## 客户端管理

### 客户端字段说明

| 字段 | 说明 | 注意事项 |
|------|------|----------|
| 客户端ID | 后端生成，用于前端登录校验和接口加密 | 无法修改，不要删除默认数据 |
| 客户端Key | 前端自定义 | 无法修改，不要删除默认数据 |
| 客户端密钥 | 前端自定义 | 无法修改，不要删除默认数据 |
| 授权类型 | 密码认证、短信认证、邮件认证、小程序认证、第三方认证 | 根据授权类型判断是否支持该登录方式 |
| 设备类型 | PC端、APP端 | 区分不同设备 |
| Token活跃超时时间 | 无操作过期时间（秒），默认30分钟 | 可自定义 |
| Token固定超时时间 | 必定过期时间（秒），默认7天 | 可自定义 |

### 新增客户端

**步骤一**: 在系统管理 → 客户端管理中新增客户端

```
示例: 新增小程序端
- 客户端Key: xcx
- 客户端密钥: 自定义
- 授权类型: 小程序认证
- 设备类型: APP端
```

**步骤二**: 配置前端请求头

前端需要在全局请求头 header 中增加 `clientid`:

```typescript
// 前端请求配置
headers: {
  'clientid': VITE_APP_CLIENT_ID
}
```

> ⚠️ **重要**: 不同客户端登录获取到的 token 不同，与其他端不互通

例如: APP 登录获取到的 token 无法用于 PC 端接口查询

## 新增自定义登录方式

### 完整步骤

#### Step 1: 新增字典数据

**位置**: 系统管理 → 字典管理 → 新增字典类型

```
字典类型: sys_grant_type
字典名称: 授权类型
字典键值: xcx (小程序)
字典键名: 小程序认证
```

#### Step 2: 修改客户端授权类型

**位置**: 系统管理 → 客户端管理 → 编辑客户端

在授权类型中勾选新增的登录方式。

#### Step 3: 后端新增认证策略

**位置**: `ruoyi-auth/service/impl/`

```java
package org.dromara.auth.service.impl;

import org.dromara.auth.form.LoginBody;
import org.dromara.auth.service.IAuthStrategy;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.RemoteUserService;
import org.dromara.system.api.domain.bo.RemoteUserBo;
import org.dromara.system.api.model.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 自定义认证策略
 *
 * @author dragonboat
 */
@Service(value = "xCXAuthStrategy") // 注意修改 Service 名称保证规范性
@RequiredArgsConstructor
public class XcxAuthStrategy implements IAuthStrategy {

    private final RemoteUserService remoteUserService;

    @Override
    public LoginUser login(String body) {
        // 1. 解析登录参数
        XcxLoginBody loginBody = parseBody(body, XcxLoginBody.class);

        // 2. 校验参数
        validateLoginBody(loginBody);

        // 3. 调用微信 API 获取用户信息
        // WxMaJscode2SessionResult session = wxMaService.getUserInfo(...);

        // 4. 构造查询条件
        RemoteUserBo userBo = new RemoteUserBo();
        userBo.setOpenid(loginBody.getOpenid());

        // 5. 查询用户（根据 openid 或 unionid）
        LoginUser loginUser = remoteUserService.loginByOpenid(userBo);

        // 6. 如果用户不存在，自动注册
        if (loginUser == null) {
            loginUser = registerUser(loginBody);
        }

        // 7. 记录登录信息
        LoginHelper.recordLogininfor(loginUser.getUserId(), Constants.LOGIN_SUCCESS, "小程序登录成功");

        // 8. 返回登录用户
        return loginUser;
    }

    /**
     * 参数校验
     */
    private void validateLoginBody(XcxLoginBody loginBody) {
        if (loginBody.getCode() == null || loginBody.getCode().isEmpty()) {
            throw new ServiceException("微信授权码不能为空");
        }
        // 其他校验...
    }

    /**
     * 自动注册用户
     */
    private LoginUser registerUser(XcxLoginBody loginBody) {
        // 实现自动注册逻辑
        // ...
        return loginUser;
    }
}
```

#### Step 4: 定义登录参数对象

```java
package org.dromara.auth.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 小程序登录对象
 *
 * @author dragonboat
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class XcxLoginBody extends LoginBody {

    /**
     * 微信授权码
     */
    @NotBlank(message = "微信授权码不能为空")
    private String code;

    /**
     * 微信 openid
     */
    private String openid;

    /**
     * 微信 unionid
     */
    private String unionid;
}
```

#### Step 5: 配置校验分组

```java
// 在 LoginBody 基类中定义分组
public @interface DefaultGroup {
}

// 在 XcxLoginBody 中使用
@Data
public class XcxLoginBody extends LoginBody {

    @NotBlank(message = "授权码不能为空", groups = {DefaultGroup.class})
    private String code;
}
```

## 内置认证策略参考

### 1. 密码认证

```java
/**
 * 密码认证策略
 */
@Service(value = "passwordAuthStrategy")
public class PasswordAuthStrategy implements IAuthStrategy {

    @Override
    public LoginUser login(String body) {
        PasswordLoginBody loginBody = parseBody(body, PasswordLoginBody.class);

        // 1. 校验验证码
        validateCaptcha(loginBody.getTenantId(), loginBody.getCode(), loginBody.getUuid());

        // 2. 校验用户名密码
        LoginUser loginUser = remoteUserService.login(loginBody);

        // 3. 记录登录日志
        LoginHelper.recordLogininfor(loginUser.getUserId(), Constants.LOGIN_SUCCESS, "登录成功");

        return loginUser;
    }
}
```

### 2. 短信认证

```java
/**
 * 短信认证策略
 */
@Service(value = "smsAuthStrategy")
public class SmsAuthStrategy implements IAuthStrategy {

    @Override
    public LoginUser login(String body) {
        SmsLoginBody loginBody = parseBody(body, SmsLoginBody.class);

        // 1. 校验手机号
        validatePhonenumber(loginBody.getPhonenumber());

        // 2. 校验验证码
        validateSmsCode(loginBody.getPhonenumber(), loginBody.getSmsCode());

        // 3. 查询或创建用户
        LoginUser loginUser = remoteUserService.loginByPhonenumber(loginBody.getPhonenumber());

        // 4. 记录登录日志
        LoginHelper.recordLogininfor(loginUser.getUserId(), Constants.LOGIN_SUCCESS, "短信登录成功");

        return loginUser;
    }
}
```

### 3. 邮件认证

```java
/**
 * 邮件认证策略
 */
@Service(value = "emailAuthStrategy")
public class EmailAuthStrategy implements IAuthStrategy {

    @Override
    public LoginUser login(String body) {
        EmailLoginBody loginBody = parseBody(body, EmailLoginBody.class);

        // 1. 校验邮箱格式
        validateEmail(loginBody.getEmail());

        // 2. 校验验证码
        validateEmailCode(loginBody.getEmail(), loginBody.getEmailCode());

        // 3. 查询或创建用户
        LoginUser loginUser = remoteUserService.loginByEmail(loginBody.getEmail());

        // 4. 记录登录日志
        LoginHelper.recordLogininfor(loginUser.getUserId(), Constants.LOGIN_SUCCESS, "邮件登录成功");

        return loginUser;
    }
}
```

### 4. 第三方认证

```java
/**
 * 第三方认证策略
 */
@Service(value = "socialAuthStrategy")
public class SocialAuthStrategy implements IAuthStrategy {

    @Override
    public LoginUser login(String body) {
        SocialLoginBody loginBody = parseBody(body, SocialLoginBody.class);

        // 1. 获取第三方授权信息
        SocialResponse socialResponse = getSocialResponse(loginBody);

        // 2. 绑定或创建用户
        LoginUser loginUser = remoteUserService.loginBySocial(socialResponse);

        // 3. 记录登录日志
        LoginHelper.recordLogininfor(loginUser.getUserId(), Constants.LOGIN_SUCCESS, "第三方登录成功");

        return loginUser;
    }
}
```

## 用户信息获取

### 获取登录用户

```java
// 获取完整用户信息
LoginUser user = LoginHelper.getLoginUser();

// 获取用户ID
Long userId = LoginHelper.getUserId();

// 获取用户名
String username = LoginHelper.getUsername();

// 获取租户ID
String tenantId = LoginHelper.getTenantId();

// 获取部门ID
Long deptId = LoginHelper.getDeptId();

// 获取用户类型
UserType userType = LoginHelper.getUserType();

// 判断是否为超级管理员
boolean isSuperAdmin = LoginHelper.isSuperAdmin();

// 判断是否为租户管理员
boolean isTenantAdmin = LoginHelper.isTenantAdmin();

// 获取扩展属性
Object obj = LoginHelper.getExtra(key);
```

### 设置扩展属性

```java
// 在登录时设置扩展属性
LoginHelper.login(loginUser, deviceType);

// 设置扩展属性
LoginHelper.setExtra("clientId", clientId);
LoginHelper.setExtra("customKey", customValue);
```

## 内网鉴权

### 功能介绍

防止外部请求访问内部服务应用：
- 请求经过 Gateway 网关会生成 `id-token`
- 后续服务校验 `id-token`
- 若未经过网关直接访问内网服务，会出现 `id-token无效` 异常

### 开启/关闭内网鉴权

**位置**: `application-common.yml`

```yaml
sa-token:
  # 开启内网鉴权
  check-id-token: true
```

### 放行内网鉴权

**位置**: `ruoyi-common-security/SecurityConfiguration`

```java
@Configuration
public class SecurityConfiguration {

    /**
     * 放行内网鉴权路径
     */
    private static final String[] IGNORE_URLS = {
        "/auth/login",
        "/auth/logout",
        "/auth/register",
        // 添加其他需要放行的路径
    };
}
```

## 登录流程

### 完整流程

```
1. 前端发起登录请求
   POST /auth/login
   Headers:
     clientid: xxx
     Content-Type: application/json
   Body:
     {
       "grantType": "password",
       "username": "admin",
       "password": "xxx",
       "code": "1234",
       "uuid": "xxx"
     }

2. Gateway 路由到 ruoyi-auth 服务

3. SysLoginService 根据 grantType 选择认证策略

4. 认证策略执行登录逻辑
   - 校验参数
   - 调用 RemoteUserService 查询/创建用户
   - 记录登录日志
   - 构建 LoginUser 对象

5. Sa-Token 生成 token

6. 返回 token 给前端

7. 前端后续请求携带 token
   Headers:
     Authorization: Bearer {token}
     clientid: xxx
```

### 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 登录成功 |
| 401 | 验证码错误、用户名密码错误、token 无效 |
| 403 | 无权限、账号被禁用 |
| 500 | 服务器错误 |

## 常见问题

### 问题 1: 登录后接口返回 401

**可能原因**:
1. Token 未携带或格式错误
2. Token 已过期
3. 客户端 ID 不匹配

**解决方案**:
```typescript
// 检查前端 token 携带
headers: {
  'Authorization': `Bearer ${token}`,
  'clientid': clientId
}
```

### 问题 2: 新增登录方式不生效

**可能原因**:
1. 字典数据未添加
2. 客户端授权类型未勾选
3. Service 名称不规范

**解决方案**:
```java
// 检查 Service 名称规范
// ✅ 正确
@Service(value = "xCXAuthStrategy")

// ❌ 错误
@Service
public class XcxAuthStrategy implements IAuthStrategy {
    // value 属性缺失
}
```

### 问题 3: 内网鉴权报错

**可能原因**:
1. 未配置放行路径
2. Gateway 未正确配置

**解决方案**:
```yaml
# 检查内网鉴权配置
sa-token:
  check-id-token: true

# 检查放行路径
SecurityConfiguration.IGNORE_URLS
```

## 安全建议

### 1. 密码安全

- ✅ 使用 `@EncryptField` 加密存储
- ✅ 使用 HTTPS 传输
- ✅ 定期强制修改密码
- ❌ 禁止明文传输密码

### 2. Token 安全

- ✅ Token 设置合理过期时间
- ✅ 敏感操作二次验证
- ✅ 登出时销毁 Token
- ❌ 禁止 Token 永久有效

### 3. 验证码

- ✅ 登录使用验证码
- ✅ 验证码定期刷新
- ✅ 验证码使用后失效
- ❌ 禁止固定验证码

### 4. 内网鉴权

- ✅ 生产环境开启内网鉴权
- ✅ 合理配置放行路径
- ✅ 定期审计放行规则
- ❌ 禁止关闭内网鉴权

## 参考资源

- [Sa-Token 官方文档](https://sa-token.cc/doc.html#/use/login-auth)
- [RuoYi-Cloud-Plus 用户文档](https://plus-doc.dromara.org/ruoyi-cloud-plus/framework/basic/user.html)
- [RuoYi-Cloud-Plus 客户端文档](https://plus-doc.dromara.org/ruoyi-cloud-plus/framework/basic/client.html)
- [RuoYi-Cloud-Plus 内网鉴权文档](https://plus-doc.dromara.org/ruoyi-cloud-plus/framework/association/inner_authentication.html)

## 版本历史

- v1.0.0 (2026-01-17) - 初始版本，基于 RuoYi-Cloud-Plus 2.X
