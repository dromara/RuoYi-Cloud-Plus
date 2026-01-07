<img src="https://foruda.gitee.com/images/1679673780944866919/d908a86f_1766278.png" width="56%" height="56%">
<div style="height: 10px; clear: both;"></div>

使用**Ruoyi-Cloud-Plus**项目作为底座进行整体的项目开发流程

- - -
## 平台简介

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![使用IntelliJ IDEA开发维护](https://img.shields.io/badge/IntelliJ%20IDEA-提供支持-blue.svg)](https://www.jetbrains.com/?from=RuoYi-Cloud-Plus)
<br>
[![RuoYi-Cloud-Plus](https://img.shields.io/badge/RuoYi_Cloud_Plus-2.5.2-success.svg)](https://gitee.com/dromara/RuoYi-Cloud-Plus)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-blue.svg)]()
[![JDK-17](https://img.shields.io/badge/JDK-17-green.svg)]()
[![JDK-21](https://img.shields.io/badge/JDK-21-green.svg)]()

> 文档地址: [plus-doc](https://plus-doc.dromara.org) 国内加速: [plus-doc.top](https://plus-doc.top)

## Quick Start

使用框架前请仔细阅读文档重点注意事项

- 初始化项目：[https://plus-doc.dromara.org/#/ruoyi-cloud-plus/quickstart/init](https://plus-doc.dromara.org/#/ruoyi-cloud-plus/quickstart/init)
- 参考文档：[https://plus-doc.dromara.org](https://plus-doc.dromara.org)

项目整体运行环境

- java 17/21

### 怎么启动

> 项目初期先简单为主，使用`mysql8.0`进行开发，后期切到`postgres14`进行交付

1. 启动基础运行环境

项目整体以DDD为导向，数据库存储持久化运行数据，redis负责分布式cache，rabbitmq负责监听数据变化

进入`script/docker`，启动基础环境(或者用idea自带的容器管理功能也可以)

```bash
$ cd script/docker
$ docker compose -f ./docker-compose.dev.yml -d # 如果docker版本不够高的话用docker-compose
```

确认服务正常启动无问题即可（compose文件中的端口如果有冲突请自行修改compose文件以及附属config文件中的端口/用户名/密码相关配置）

![img.png](doc/img_5.png)

2. 导入`script/sql/ry-config.sql`（以及其他同级目录下的sql文件）

> ry-config是默认的数据库名称，也是各个配置文件中的默认内容，如果在开发过程中有冲突等问题使用不了这个数据库名称，请附带修改各种config文件中的内容

> 这里只演示ry-config.sql，其余文件自行导入数据库即可

> 同时在这之前请确保安装了mysql client

```bash
$ mysql -h127.0.0.1 -P3306 -uroot -p'root' -e 'CREATE DATABASE IF NOT EXISTS `ry-config` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;' 
$ mysql -u 用户名 -p -h 主机地址 ry-config < script/sql/ry-config.sql
```

3. 导入nacos配置

参考上述文档中的内容修改`script/config`下的配置文件（数据库地址，用户名，密码，code gen master等内容）后

修改`ruoyi-visual/ruoyi-nacos/src/resources/application.properties:L42-45`相关的数据库配置内容

```bash
$ cd script/
$ python3 sync_nacos.py --dir ./config --host http://你的ip:8848
```

即可自动向nacos中导入需要分发的配置文件

4. 启动基础模块

![img.png](doc/img_6.png)

红框内为必须第一个启动的，黄框为必须第二步全部启动的，蓝框为拓展模块可选启动

### 如何快速进行一个模块的开发

> 推荐全程idea进行操作，分布式的系统用其他编辑器很容易出现依赖找不全的问题

1. 拷贝一份`ruoyi-system`

![img.png](doc/img.png)
![img.png](doc/img1.png)
![img.png](doc/img_1.png)

2. 修改pom.xml

![img.png](doc/img_2.png)

3. 修改spring boot entrypoint

![img.png](doc/img_3.png)

确认idea的spring启动项中有配置好的启动内容即可进行开发

![img.png](doc/img_4.png)

## 软件架构图

![Plus部署架构图](https://foruda.gitee.com/images/1678980131147747524/5c2d5a5c_1766278.png "Plus部署架构图.png")

### 单模块通用数据流

```mermaid
graph TD
    User(Client/Frontend)
    
    subgraph "API Gateway (ruoyi-gateway)"
        Auth[Auth Filters]
        Route[Router]
    end
    
    subgraph "Business Module (e.g. ruoyi-system / dragonboat-quote)"
        subgraph "Controller Layer (Web)"
            Ctrl[Controller]
            Val[Validator @Validated]
        end
        
        subgraph "Service Layer (Logic)"
            SvcInterface[IService Interface]
            SvcImpl[ServiceImpl]
            Trans[Transactional Manager]
        end
        
        subgraph "Persistence Layer (Data)"
            Mapper[BaseMapperPlus]
            MP[MyBatis-Plus Engine]
        end
        
        style Ctrl fill:#e1f5fe
        style SvcImpl fill:#fff3e0
        style Mapper fill:#e8f5e9
    end
    
    subgraph "Infrastructure"
        DB[(MySQL Database)]
        Cache[(Redis Cache)]
    end
    %% Flow
    User -->|HTTP Request| Auth
    Auth --> Route
    Route -->|Load Balance| Ctrl
    
    Ctrl -->|1. Validate DTO/BO| Val
    Val -->|2. Call Service| SvcInterface
    
    SvcInterface --> SvcImpl
    SvcImpl -->|3. Business Logic| SvcImpl
    SvcImpl -.->|Optional: Check Cache| Cache
    
    SvcImpl -->|4. CRUD Operation| Mapper
    Mapper -->|5. Execute SQL| MP
    MP --> DB
    
    DB -->|Return Entity| MP
    MP --> Mapper
    Mapper --> SvcImpl
    
    SvcImpl -->|6. Convert Entity to VO| SvcImpl
    SvcImpl -->|Return VO| Ctrl
    Ctrl -->|Return R&lt;VO&gt;| User
```