## 平台简介(开发中)
[![码云Gitee](https://gitee.com/JavaLionLi/RuoYi-Cloud-Plus/badge/star.svg?theme=blue)](https://gitee.com/JavaLionLi/RuoYi-Cloud-Plus)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://gitee.com/JavaLionLi/RuoYi-Cloud-Plus/blob/master/LICENSE)
<br>
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.5-blue.svg)]()
[![JDK-8+](https://img.shields.io/badge/JDK-8-green.svg)]()
[![JDK-11](https://img.shields.io/badge/JDK-11-green.svg)]()

> RuoYi-Cloud-Plus 是重写 RuoYi-Cloud 全方位升级(不兼容原框架)

| 功能介绍 | 使用技术 | 文档地址 | 特性注意事项 |
|---|---|---|---|
| 前端开发框架 | Vue、Element UI | [Element UI官网](https://element.eleme.cn/#/zh-CN) | |
| 后端开发框架 | SpringBoot | [SpringBoot官网](https://spring.io/projects/spring-boot/#learn) | |
| 容器框架 | Undertow | [Undertow官网](https://undertow.io/) | 基于 XNIO 的高性能容器 |
| 权限认证框架 | Spring Security、Jwt | [SpringSecurity官网](https://spring.io/projects/spring-security#learn) | 支持多终端认证系统 |
| 权限认证框架 | Sa-Token、Jwt | [Sa-Token官网](https://sa-token.dev33.cn/) | 强解耦、强扩展 |
| 关系数据库 | MySQL | [MySQL官网](https://dev.mysql.com/) | 适配 8.X 最低 5.7 |
| 缓存数据库 | Redis | [Redis官网](https://redis.io/) | 适配 6.X 最低 4.X |
| 数据库框架 | Mybatis-Plus | [Mybatis-Plus文档](https://baomidou.com/guide/) | 快速 CRUD 增加开发效率 |
| 数据库框架 | p6spy | [p6spy官网](https://p6spy.readthedocs.io/) | 更强劲的 SQL 分析 |
| 多数据源框架 | dynamic-datasource | [dynamic-ds文档](https://www.kancloud.cn/tracy5546/dynamic-datasource/content) | 支持主从与多种类数据库异构 |
| 序列化框架 | Jackson | [Jackson官网](https://github.com/FasterXML/jackson) | 统一使用 jackson 高效可靠 |
| Redis客户端 | Redisson | [Redisson文档](https://github.com/redisson/redisson/wiki/%E7%9B%AE%E5%BD%95) | 支持单机、集群配置 |
| 分布式限流 | Redisson | [Redisson文档](https://github.com/redisson/redisson/wiki/%E7%9B%AE%E5%BD%95) | 全局、请求IP、集群ID 多种限流 |
| 分布式锁 | Lock4j | [Lock4j官网](https://gitee.com/baomidou/lock4j) | 注解锁、工具锁 多种多样 |
| 分布式幂等 | Redisson | [Lock4j文档](https://gitee.com/baomidou/lock4j) | 拦截重复提交 |
| 分布式任务调度 | Xxl-Job | [Xxl-Job官网](https://www.xuxueli.com/xxl-job/) | 高性能 高可靠 易扩展 |
| 文件存储 | Minio | [Minio文档](https://docs.min.io/) | 本地存储 |
| 文件存储 | 七牛、阿里、腾讯 | [OSS使用文档](https://gitee.com/JavaLionLi/RuoYi-Vue-Plus/wikis/pages?sort_id=4359146&doc_id=1469725) | 云存储 |
| 监控框架 | SpringBoot-Admin | [SpringBoot-Admin文档](https://codecentric.github.io/spring-boot-admin/current/) | 全方位服务监控 |
| 校验框架 | Validation | [Validation文档](https://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/) | 增强接口安全性、严谨性 支持国际化 |
| Excel框架 | Alibaba EasyExcel | [EasyExcel文档](https://www.yuque.com/easyexcel/doc/easyexcel) | 性能优异 扩展性强 |
| 文档框架 | Knife4j | [Knife4j文档](https://doc.xiaominfo.com/knife4j/documentation/) | 美化接口文档 |
| 工具类框架 | Hutool、Lombok | [Hutool文档](https://www.hutool.cn/docs/) | 减少代码冗余 增加安全性 |
| 代码生成器 | 适配MP、Knife4j规范化代码 | [Hutool文档](https://www.hutool.cn/docs/) | 一键生成前后端代码 |
| 部署方式 | Docker | [Docker文档](https://docs.docker.com/) | 容器编排 一键部署业务集群 |
| 国际化 | SpringMessage | [SpringMVC文档](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc) | Spring标准国际化方案 |


## 系统模块

~~~
com.ruoyi     
├── ruoyi-ui              // 前端框架 [80]
├── ruoyi-gateway         // 网关模块 [8080]
├── ruoyi-auth            // 认证中心 [9200]
├── ruoyi-api             // 接口模块
│       └── ruoyi-api-system                          // 系统接口
├── ruoyi-common          // 通用模块
│       └── ruoyi-common-core                         // 核心模块
│       └── ruoyi-common-datascope                    // 权限范围
│       └── ruoyi-common-datasource                   // 多数据源
│       └── ruoyi-common-log                          // 日志记录
│       └── ruoyi-common-redis                        // 缓存服务
│       └── ruoyi-common-security                     // 安全模块
│       └── ruoyi-common-swagger                      // 系统接口
├── ruoyi-modules         // 业务模块
│       └── ruoyi-system                              // 系统模块 [9201]
│       └── ruoyi-gen                                 // 代码生成 [9202]
│       └── ruoyi-job                                 // 定时任务 [9203]
│       └── ruoyi-file                                // 文件服务 [9300]
├── ruoyi-visual          // 图形化管理模块
│       └── ruoyi-visual-monitor                      // 监控中心 [9100]
├──pom.xml                // 公共依赖
~~~

## 软件架构图

<img src="https://oscimg.oschina.net/oscnet/up-82e9722ecb846786405a904bafcf19f73f3.png"/>

## 贡献代码

欢迎各路英雄豪杰 `PR` 代码 请提交到 `dev` 开发分支 统一测试发版

### 其他

* 同步升级 RuoYi-Cloud
* 分离版分支 [RuoYi-Vue-Plus](https://gitee.com/JavaLionLi/RuoYi-Vue-Plus)
* 单模块 fast 分支 [RuoYi-Vue-Plus-fast](https://gitee.com/JavaLionLi/RuoYi-Vue-Plus/tree/fast/)
* satoken 分支 [RuoYi-Vue-Plus-satoken](https://gitee.com/JavaLionLi/RuoYi-Vue-Plus/tree/satoken/)

## 捐献作者
作者为兼职做开源,平时还需要工作,如果帮到了您可以请作者吃个盒饭  
<img src="https://images.gitee.com/uploads/images/2021/0525/101654_451e4523_1766278.jpeg" width="300px" height="450px" />
<img src="https://images.gitee.com/uploads/images/2021/0525/101713_3d18b119_1766278.jpeg" width="300px" height="450px" />

## 业务功能

| 功能 | 介绍 |
|---|---|
| 用户管理 | 用户是系统操作者，该功能主要完成系统用户配置。 |
| 部门管理 | 配置系统组织机构（公司、部门、小组），树结构展现支持数据权限。 |
| 岗位管理 | 配置系统用户所属担任职务。 |
| 菜单管理 | 配置系统菜单，操作权限，按钮权限标识等。 |
| 角色管理 | 角色菜单权限分配、设置角色按机构进行数据范围权限划分。 |
| 字典管理 | 对系统中经常使用的一些较为固定的数据进行维护。 |
| 参数管理 | 对系统动态配置常用参数。 |
| 通知公告 | 系统通知公告信息发布维护。 |
| 操作日志 | 系统正常操作日志记录和查询；系统异常信息日志记录和查询。 |
| 登录日志 | 系统登录日志记录查询包含登录异常。 |
| 文件管理 | 系统文件上传、下载等管理。 |
| 定时任务 | 在线（添加、修改、删除)任务调度包含执行结果日志。 |
| 代码生成 | 前后端代码的生成（java、html、xml、sql）支持CRUD下载 。 |
| 系统接口 | 根据业务代码自动生成相关的api接口文档。 |
| 服务监控 | 监视集群系统CPU、内存、磁盘、堆栈、在线日志、Spring相关配置等。 |
| 缓存监控 | 对系统的缓存信息查询，命令统计等。 |
| 在线构建器 | 拖动表单元素生成相应的HTML代码。 |
| 连接池监视 | 监视当前系统数据库连接池状态，可进行分析SQL找出系统性能瓶颈。 |
| 使用案例 | 系统的一些功能案例 |

## 演示图例

<table border="1" cellpadding="1" cellspacing="1" style="width:500px">
	<tbody>
		<tr>
			<td><img src="https://oscimg.oschina.net/oscnet/up-972235bcbe3518dedd351ff0e2ee7d1031c.png" width="1920" /></td>
			<td><img src="https://oscimg.oschina.net/oscnet/up-5e0097702fa91e2e36391de8127676a7fa1.png" width="1920" /></td>
		</tr>
		<tr>
			<td>
			<p><img src="https://oscimg.oschina.net/oscnet/up-e56e3828f48cd9886d88731766f06d5f3c1.png" width="1920" /></p>
			</td>
			<td><img src="https://oscimg.oschina.net/oscnet/up-0715990ea1a9f254ec2138fcd063c1f556a.png" width="1920" /></td>
		</tr>
		<tr>
			<td><img src="https://oscimg.oschina.net/oscnet/up-eaf5417ccf921bb64abb959e3d8e290467f.png" width="1920" /></td>
			<td><img src="https://oscimg.oschina.net/oscnet/up-fc285cf33095ebf8318de6999af0f473861.png" width="1920" /></td>
		</tr>
		<tr>
			<td><img src="https://oscimg.oschina.net/oscnet/up-60c83fd8bd61c29df6dbf47c88355e9c272.png" width="1920" /></td>
			<td><img src="https://oscimg.oschina.net/oscnet/up-7f731948c8b73c7d90f67f9e1c7a534d5c3.png" width="1920" /></td>
		</tr>
		<tr>
			<td><img src="https://oscimg.oschina.net/oscnet/up-e4de89b5e2d20c52d3c3a47f9eb88eb8526.png" width="1920" /></td>
			<td><img src="https://oscimg.oschina.net/oscnet/up-8791d823a508eb90e67c604f36f57491a67.png" width="1920" /></td>
		</tr>
		<tr>
			<td><img src="https://oscimg.oschina.net/oscnet/up-4589afd99982ead331785299b894174feb6.png" width="1920" /></td>
			<td><img src="https://oscimg.oschina.net/oscnet/up-8ea177cdacaea20995daf2f596b15232561.png" width="1920" /></td>
		</tr>
		<tr>
			<td><img src="https://oscimg.oschina.net/oscnet/up-32d1d04c55c11f74c9129fbbc58399728c4.png" width="1920" /></td>
			<td><img src="https://oscimg.oschina.net/oscnet/up-04fa118f7631b7ae6fd72299ca0a1430a63.png" width="1920" /></td>
		</tr>
		<tr>
			<td><img src="https://oscimg.oschina.net/oscnet/up-fe7e85b65827802bfaadf3acd42568b58c7.png" width="1920" /></td>
			<td><img src="https://oscimg.oschina.net/oscnet/up-eff2b02a54f8188022d8498cfe6af6fcc06.png" width="1920" /></td>
		</tr>
	</tbody>
</table>
