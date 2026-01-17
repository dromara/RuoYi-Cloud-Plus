# RuoYi-Cloud-Plus 新微服务模块创建指南

适用于在 RuoYi-Cloud-Plus 项目中创建新的业务微服务模块。

## 触发词

- "创建新模块"
- "new module"
- "创建微服务"
- "new microservice"
- "新建服务"
- "create module"

## 新模块创建工作流

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     新微服务模块创建工作流                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. 规划决策                                                                  │
│     ↓                                                                       │
│  2. 创建模块结构 (ry-module-create)                                           │
│     ↓                                                                       │
│  3. 配置 Nacos & Gateway                                                     │
│     ↓                                                                       │
│  4. 配置 Seata (如需要)                                                       │
│     ↓                                                                       │
│  5. 验证启动                                                                 │
│     ↓                                                                       │
│  6. 业务开发 (ry-workflow)                                                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 阶段 1: 规划决策

### 1.1 确定是否需要新模块

**考虑以下问题**:
- [ ] 是否需要独立的数据库？
- [ ] 是否需要独立的部署单元？
- [ ] 是否与现有模块边界清晰？
- [ ] 是否需要独立的 API 网关路由？

**如果都是"是"，则创建新模块；否则在现有模块中添加功能。**

**相关 Skills**:
- `/skill ry-workflow` - 查看完整开发流程

### 1.2 确定模块特性

**决策清单**:
- [ ] 是否需要多租户？ → 参考: `/skill ry-multi-tenant`
- [ ] 是否需要数据权限？ → 参考: `/skill ry-data-permission`
- [ ] 是否需要多数据源？ → 参考: `/skill ry-multi-datasource`
- [ ] 是否需要自定义登录？ → 参考: `/skill ry-custom-auth`
- [ ] 是否需要 Dubbo API？ → 参考: `/skill ry-dubbo-api`

### 1.3 命名规范

**模块命名**: `ruoyi-{feature}`

```
示例:
ruoyi-product     (产品管理)
ruoyi-order       (订单管理)
ruoyi-payment     (支付管理)
ruoyi-workflow    (工作流管理)
```

**包命名**: `org.dromara.{feature}`

```
示例:
org.dromara.product
org.dromara.order
org.dromara.payment
```

## 阶段 2: 创建模块结构

### 步骤 2.1: 复制现有模块（推荐）

```bash
# 复制 ruoyi-system 作为模板
cp -r ruoyi-modules/ruoyi-system ruoyi-modules/ruoyi-{new-module}
```

**为什么要复制 ruoyi-system？**
- 包含所有必需的依赖
- 目录结构完整
- 配置文件齐全
- 示例代码可参考

### 步骤 2.2: 修改 pom.xml

**位置**: `ruoyi-modules/ruoyi-{new-module}/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.dromara</groupId>
        <artifactId>ruoyi-modules</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>ruoyi-{new-module}</artifactId>
    <version>${revision}</version>

    <description>
        {新模块描述}
    </description>

    <dependencies>
        <!-- Nacos 注册中心和配置中心 -->
        <dependency>
            <groupId>org.dromara</groupId>
            <artifactId>ruoyi-common-nacos</artifactId>
        </dependency>

        <!-- MyBatis-Plus & 数据权限 -->
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

        <!-- 数据脱敏 -->
        <dependency>
            <groupId>org.dromara</groupId>
            <artifactId>ruoyi-common-sensitive</artifactId>
        </dependency>

        <!-- 字段翻译 -->
        <dependency>
            <groupId>org.dromara</groupId>
            <artifactId>ruoyi-common-translation</artifactId>
        </dependency>

        <!-- 根据需要添加其他依赖 -->
        <!-- 多数据源: 根据实际情况配置 -->
        <!-- API 模块: 根据需要添加 ruoyi-api-system 等 -->
    </dependencies>
</project>
```

> **⚠️ 注意**: 如果模块不需要多租户，可以移除 `ruoyi-common-tenant` 依赖，但需要在配置文件中排除多租户插件。

**相关 Skills**:
- `/skill ry-multi-tenant` - 了解多租户配置
- `/skill ry-multi-datasource` - 了解多数据源配置

### 步骤 2.3: 修改包名和类名

**批量替换包名**:
```bash
cd ruoyi-modules/ruoyi-{new-module}

# 替换 Java 文件中的包名
find . -type f -name "*.java" -exec sed -i 's/org\.dromara\.system/org.dromara.{new-module}/g' {} +

# 替换 XML 文件中的包名
find . -type f -name "*.xml" -exec sed -i 's/org\.dromara\.system/org.dromara.{new-module}/g' {} +

# 替换 YAML 文件中的包名
find . -type f -name "*.yml" -exec sed -i 's/org\.dromara\.system/org.dromara.{new-module}/g' {} +
```

**类名映射**:
```
RuoYiSystemApplication  →  RuoYi{NewModule}Application
SystemUserController    →  {NewModule}UserController
SystemService          →  I{NewModule}Service
```

### 步骤 2.4: 修改启动类

**位置**: `src/main/java/org/dromara/{new-module}/RuoYi{NewModule}Application.java`

```java
package org.dromara.{new-module};

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * {新模块名称}启动类
 *
 * @author {your-name}
 */
@EnableDubbo
@SpringBootApplication
public class RuoYi{NewModule}Application {

    public static void main(String[] args) {
        SpringApplication.run(RuoYi{NewModule}Application.class, args);
        System.out.println("(♥◠‿◠)ノ゙  {新模块名称}启动成功   ლ(´ڡ`ლ)゙");
    }
}
```

> **✅ 检查**: 确保 `@EnableDubbo` 注解存在，否则服务无法注册到 Nacos

### 步骤 2.5: 修改配置文件

**application.yml**:
```yaml
spring:
  application:
    name: @artifactId@
  profiles:
    active: @profiles.active@

server:
  port: @server.port@
```

**bootstrap.yml**:
```yaml
spring:
  application:
    name: @artifactId@
  cloud:
    nacos:
      server-addr: @nacos.addr@
      config:
        namespace: @nacos.namespace@
        group: @nacos.group@
        file-extension: yml
        shared-configs:
          - application-{profile}.yaml
      discovery:
        namespace: @nacos.namespace@
        group: @nacos.group@
```

### 步骤 2.6: 清理示例代码

**保留的结构**:
```
src/main/java/org/dromara/{new-module}/
├── controller/           # 保留空目录或示例控制器
├── service/             # 保留空目录或示例服务
├── mapper/              # 保留空目录或示例 Mapper
├── domain/              # 清空，重新生成
├── listener/            # 保留事件监听器（如有）
└── dubbo/               # 清空，重新生成
```

**删除的业务代码**:
```bash
# 删除原业务代码
rm -rf src/main/java/org/dromara/{new-module}/controller/system/*
rm -rf src/main/java/org/dromara/{new-module}/domain/*
rm -rf src/main/java/org/dromara/{new-module}/service/*
rm -rf src/main/java/org/dromara/{new-module}/mapper/*
rm -rf src/main/java/org/dromara/{new-module}/dubbo/*
```

**相关 Skills**:
- `/skill ry-crud` - 了解如何生成 CRUD 代码
- `/skill ry-code-generator` - 了解代码生成器使用

### 步骤 2.7: 创建标准目录结构

```bash
cd ruoyi-modules/ruoyi-{new-module}/src/main/java/org/dromara/{new-module}

# 创建标准 DDD 分层目录
mkdir -p controller/{feature}
mkdir -p service
mkdir -p service/impl
mkdir -p mapper
mkdir -p domain/bo
mkdir -p domain/vo
mkdir -p domain/convert
mkdir -p dubbo

# 验证目录结构
tree -L 3 .
```

**预期结构**:
```
org.dromara.{new-module}/
├── controller/
│   └── {feature}/          # 按功能分组的控制器
├── service/
│   ├── I{Xxx}Service.java  # 服务接口
│   └── impl/
│       └── {Xxx}ServiceImpl.java  # 服务实现
├── mapper/
│   └── {Xxx}Mapper.java    # MyBatis-Plus Mapper
├── domain/
│   ├── {Xxx}.java         # 实体类
│   ├── bo/
│   │   └── {Xxx}Bo.java   # 业务对象
│   ├── vo/
│   │   └── {Xxx}Vo.java   # 视图对象
│   └── convert/
│       └── {Xxx}Convert.java  # 对象转换器
└── dubbo/
    └── Remote{Xxx}ServiceImpl.java  # Dubbo 服务实现
```

**相关 Skills**:
- `/skill ry-code-review` - 了解架构审查标准

## 阶段 3: 配置 Nacos & Gateway

### 步骤 3.1: 创建 Nacos 配置

**配置命名规则**: `{artifactId}-{profile}.yaml`

```
示例:
ruoyi-product-dev.yaml       # 产品模块开发环境
ruoyi-product-test.yaml      # 产品模块测试环境
ruoyi-product-prod.yaml      # 产品模块生产环境
```

**基础配置模板**:
```yaml
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/ry-cloud?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8
    username: root
    password: root

  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0
      timeout: 10s
      lettuce:
        pool:
          min-idle: 0
          max-idle: 8
          max-active: 8
          max-wait: -1ms

# MyBatis-Plus 配置
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*Mapper.xml
  type-aliases-package: org.dromara.{new-module}.domain
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: true
  global-config:
    db-config:
      id-type: auto

# Dubbo 配置
dubbo:
  application:
    name: @artifactId@
  protocol:
    name: dubbo
    port: -1
  registry:
    address: nacos://${spring.cloud.nacos.server-addr}
    group: ${spring.cloud.nacos.discovery.group}
  scan:
    base-packages: org.dromara.{new-module}.dubbo
```

**多租户配置** (如需要):
```yaml
# 多租户配置（参考 ry-multi-tenant）
tenant:
  enable: true
  excludes:
    - sys_user
    - sys_dept
    # 添加非租户表...
```

**相关 Skills**:
- `/skill ry-multi-tenant` - 多租户配置详情

### 步骤 3.2: 配置 Gateway 路由

**位置**: Nacos 配置中心的 `ruoyi-gateway.yml`

```yaml
spring:
  cloud:
    gateway:
      routes:
        # 新模块路由配置
        - id: ruoyi-{new-module}
          uri: lb://ruoyi-{new-module}
          predicates:
            - Path=/{new-module}/**
          filters:
            - StripPrefix=1
```

**路由命名规范**: `{module}`

```
示例:
/product   → ruoyi-product
/order     → ruoyi-order
/payment   → ruoyi-payment
```

**访问路径**: `http://gateway:port/{module}/controller/action`

```
示例:
http://localhost:8080/product/product/list
http://localhost:8080/order/order/create
```

## 阶段 4: 配置 Seata (分布式事务)

### 步骤 4.1: 判断是否需要 Seata

**需要 Seata 的场景**:
- [ ] 模块间需要分布式事务
- [ ] 跨服务数据一致性要求高
- [ ] 多数据源事务协调

**相关 Skills**:
- `/skill ry-multi-datasource` - 事务注解选择

### 步骤 4.2: 配置 Seata 事务组

**位置**: Nacos 配置中心的 `seata-server.properties`

```properties
# 添加新模块的事务组
service.vgroupMapping.ruoyi-{new-module}_tx_group=default
```

**Service 中使用**:
```java
@GlobalTransactional(rollbackFor = Exception.class)
public void distributedTransaction() {
    // 分布式事务代码
}
```

## 阶段 5: 验证启动

### 步骤 5.1: 启动顺序检查

**启动前置服务**:
```bash
# 1. 基础设施
Nacos
MySQL
Redis

# 2. 框架服务
ruoyi-gateway    # 网关
ruoyi-auth      # 认证

# 3. 新模块
ruoyi-{new-module}
```

### 步骤 5.2: 创建 IDEA 运行配置

**位置**: `.run/ruoyi-{new-module}.run.xml`

```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="ruoyi-{new-module}" type="SpringBootApplicationConfigurationType" factoryName="Spring Boot">
    <module name="ruoyi-{new-module}" />
    <option name="SPRING_BOOT_MAIN_CLASS" value="org.dromara.{new-module}.RuoYi{NewModule}Application" />
    <option name="ACTIVE_PROFILES" value="dev" />
    <option name="ALTERNATIVE_JRE_ENABLED" value="true" />
    <option name="ALTERNATIVE_JRE_PATH" value="17" />
    <extension name="coverage">
      <pattern>
        <option name="PATTERN" value="org.dromara.{new-module}.*" />
        <option name="ENABLED" value="true" />
      </pattern>
    </extension>
    <method v="2">
      <option name="Make" enabled="true" />
    </method>
  </configuration>
</component>
```

### 步骤 5.3: 验证检查清单

**Nacos 注册检查**:
```bash
# 1. 登录 Nacos 控制台
# 2. 进入 服务管理 → 服务列表
# 3. 查找 ruoyi-{new-module}
# 4. 确认实例数 = 1
```

**Gateway 路由检查**:
```bash
# 1. 进入 Nacos 配置中心
# 2. 查看 ruoyi-gateway.yml
# 3. 确认路由配置存在
```

**健康检查**:
```bash
# 访问健康检查接口
curl http://localhost:{port}/actuator/health

# 预期响应:
{
  "status": "UP"
}
```

## 阶段 6: 业务开发

模块创建成功后，进入业务开发阶段：

### 步骤 6.1: 设计数据库

**相关 Skills**:
- `/skill ry-multi-tenant` - 多租户表设计
- `/skill ry-data-permission` - 数据权限字段设计

### 步骤 6.2: 生成代码

**相关 Skills**:
- `/skill ry-code-generator` - 使用代码生成器
- `/skill ry-crud` - CRUD 开发指导

### 步骤 6.3: 实现 Dubbo API (可选)

**相关 Skills**:
- `/skill ry-dubbo-api` - Dubbo API 开发

### 步骤 6.4: 业务实现

**相关 Skills**:
- `/skill ry-workflow` - 完整开发流程
- `/skill ry-multi-datasource` - 多数据源配置
- `/skill ry-custom-auth` - 自定义登录

### 步骤 6.5: 代码提交和审查

**相关 Skills**:
- `/skill ry-commit-standards` - Commit 规范
- `/skill ry-code-review` - 代码审查标准
- `/skill create-pr` - 创建 PR

## 完整示例：创建产品模块

### 示例: 创建 ruoyi-product 模块

```bash
# 1. 复制模块
cp -r ruoyi-modules/ruoyi-system ruoyi-modules/ruoyi-product

# 2. 替换包名
cd ruoyi-modules/ruoyi-product
find . -type f -name "*.java" -exec sed -i 's/org\.dromara\.system/org.dromara.product/g' {} +

# 3. 修改 pom.xml
# artifactId: ruoyi-product

# 4. 修改启动类
# RuoYiSystemApplication → RuoYiProductApplication

# 5. 修改配置文件
# application.yml: spring.application.name = ruoyi-product

# 6. 创建 Nacos 配置
# ruoyi-product-dev.yaml

# 7. 配置 Gateway 路由
# 添加到 ruoyi-gateway.yml

# 8. 启动测试
# 运行 RuoYiProductApplication

# 9. 验证
# Nacos: 服务已注册
# Gateway: 路由已配置
# 健康检查: 通过

# 10. 开始业务开发
# /skill ry-workflow "DB-101 产品管理"
```

## 新模块开发检查清单

### 创建阶段

- [ ] 模块命名规范 (ruoyi-{feature})
- [ ] 包名规范 (org.dromara.{feature})
- [ ] pom.xml 依赖完整
- [ ] 启动类 @EnableDubbo 注解
- [ ] application.yml 配置正确
- [ ] bootstrap.yml 配置正确
- [ ] Nacos 配置已创建
- [ ] Gateway 路由已配置
- [ ] IDEA 运行配置已创建
- [ ] 目录结构符合 DDD 规范

### 启动验证

- [ ] 基础服务已启动 (Nacos, MySQL, Redis)
- [ ] Gateway 已启动
- [ ] Auth 已启动
- [ ] 新模块启动成功
- [ ] Nacos 注册成功
- [ ] Gateway 路由生效
- [ ] 健康检查通过

### 业务开发前

- [ ] 需求分析完成
- [ ] 数据库设计完成
- [ ] 确定是否需要多租户 → `/skill ry-multi-tenant`
- [ ] 确定是否需要数据权限 → `/skill ry-data-permission`
- [ ] 确定是否需要多数据源 → `/skill ry-multi-datasource`
- [ ] 确定是否需要 Dubbo API → `/skill ry-dubbo-api`

## 常见问题

### Q1: 新模块启动失败，提示找不到 Nacos？

**A**: 检查:
1. Nacos 是否已启动
2. bootstrap.yml 中的 Nacos 地址是否正确
3. 网络是否连通

### Q2: 新模块注册到 Nacos 但 Gateway 访问不通？

**A**: 检查:
1. Gateway 路由配置是否正确
2. 路由的 StripPrefix 配置
3. 服务名是否与配置一致

### Q3: 新模块需要调用其他模块的 API？

**A**:
```xml
<!-- 在 pom.xml 中添加 API 依赖 -->
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-api-system</artifactId>
</dependency>
```

参考: `/skill ry-dubbo-api`

### Q4: 新模块不需要多租户怎么办？

**A**:
1. 移除 pom.xml 中的 `ruoyi-common-tenant` 依赖
2. 或者保留依赖，在配置中排除表

参考: `/skill ry-multi-tenant`

## 相关技能索引

| 阶段 | 需要参考的 Skills |
|------|------------------|
| 规划 | ry-workflow, ry-multi-tenant, ry-data-permission |
| 创建 | ry-module-create, ry-code-review |
| 配置 | ry-multi-tenant, ry-multi-datasource |
| 验证 | 无 |
| 开发 | ry-workflow, ry-code-generator, ry-crud, ry-dubbo-api |
| 审查 | ry-commit-standards, ry-code-review |

## 版本历史

- v2.0.0 (2026-01-17) - 重写，增加 Skill 引用和工作流集成
- v1.0.0 (2026-01-17) - 初始版本
