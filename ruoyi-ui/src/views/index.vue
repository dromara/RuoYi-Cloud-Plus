<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="12" style="padding-left: 20px">
        <h2>RuoYi-Cloud-Plus后台管理系统</h2>
        <p>
          RuoYi-Cloud-Plus 微服务通用权限管理系统 重写 RuoYi-Cloud 全方位升级(不兼容原框架)
          <br/>
          * 前端开发框架 Vue、Element UI<br/>
          * 后端开发框架 Spring Boot<br/>
          * 微服务开发框架 Spring Cloud、Spring Cloud Alibaba<br/>
          * 容器框架 Undertow 基于 XNIO 的高性能容器<br/>
          * 权限认证框架 Sa-Token、Jwt 支持多终端认证系统<br/>
          * 关系数据库 MySQL 适配 8.X 最低 5.7<br/>
          * 关系数据库 Oracle 适配 11g 12c<br/>
          * 关系数据库 PostgreSQL 适配 13 14<br/>
          * 关系数据库 SQLServer 适配 2017 2019<br/>
          * 缓存数据库 Redis 适配 6.X 最低 5.X<br/>
          * 分布式注册中心 Alibaba Nacos 采用2.X 基于GRPC通信高性能<br/>
          * 分布式配置中心 Alibaba Nacos 采用2.X 基于GRPC通信高性能<br/>
          * 服务网关 Spring Cloud Gateway 响应式高性能网关<br/>
          * 负载均衡 Spring Cloud Loadbalancer 负载均衡处理<br/>
          * RPC远程调用 Apache Dubbo 原生态使用体验、高性能<br/>
          * 分布式限流熔断 Alibaba Sentinel 无侵入、高扩展<br/>
          * 分布式事务 Alibaba Seata 无侵入、高扩展 支持 四种模式<br/>
          * 分布式消息队列 Spring Cloud Stream 门面框架兼容各种MQ集成<br/>
          * 分布式消息队列 Apache Kafka 高性能高速度<br/>
          * 分布式消息队列 Apache RocketMQ 高可用功能多样<br/>
          * 分布式消息队列 RabbitMQ 支持各种扩展插件功能多样性<br/>
          * 分布式搜索引擎 ElasticSearch 业界知名<br/>
          * 分布式链路追踪 Apache SkyWalking 链路追踪、网格分析、度量聚合、可视化<br/>
          * 分布式日志中心 ELK 业界成熟解决方案<br/>
          * 分布式锁 Lock4j 注解锁、工具锁 多种多样<br/>
          * 分布式幂等 Redisson 拦截重复提交<br/>
          * 分布式任务调度 Xxl-Job 高性能 高可靠 易扩展<br/>
          * 分布式文件存储 Minio 本地存储<br/>
          * 分布式云存储 七牛、阿里、腾讯 云存储<br/>
          * 分布式监控 Prometheus、Grafana 全方位性能监控<br/>
          * 服务监控 SpringBoot-Admin 全方位服务监控<br/>
          * 数据库框架 Mybatis-Plus 快速 CRUD 增加开发效率<br/>
          * 数据库框架 p6spy 更强劲的 SQL 分析<br/>
          * 多数据源框架 dynamic-datasource 支持主从与多种类数据库异构<br/>
          * 序列化框架 Jackson 统一使用 jackson 高效可靠<br/>
          * Redis客户端 Redisson 性能强劲、API丰富<br/>
          * 校验框架 Validation 增强接口安全性 严谨性<br/>
          * Excel框架 Alibaba EasyExcel 性能优异 扩展性强<br/>
          * 文档框架 SpringDoc、javadoc 无注解零入侵基于java注释<br/>
          * 工具类框架 Hutool、Lombok 减少代码冗余 增加安全性<br/>
          * 代码生成器 适配MP、SpringDoc规范化代码 一键生成前后端代码<br/>
          * 部署方式 Docker 容器编排 一键部署业务集群<br/>
          * 国际化 SpringMessage Spring标准国际化方案<br/>
        </p>
        <p>
          <b>当前版本:</b> <span>v{{ version }}</span>
        </p>
        <p>
          <el-tag type="danger">&yen;免费开源</el-tag>
        </p>
        <p>
          <el-button
            type="primary"
            size="mini"
            icon="el-icon-cloudy"
            plain
            @click="goTarget('https://gitee.com/JavaLionLi/RuoYi-Cloud-Plus')"
            >访问码云</el-button
          >
          <el-button
            size="mini"
            icon="el-icon-s-home"
            plain
            @click="goTarget('https://github.com/JavaLionLi/RuoYi-Cloud-Plus')"
            >访问主页</el-button
          >
          <el-button
            type="primary"
            size="mini"
            icon="el-icon-cloudy"
            plain
            @click="goTarget('https://gitee.com/JavaLionLi/RuoYi-Cloud-Plus/wikis/pages?sort_id=5303316&doc_id=2056143')"
          >更新日志</el-button
          >
        </p>
      </el-col>

      <el-col :sm="24" :lg="12" style="padding-left: 50px">
        <el-row>
          <el-col :span="12">
            <h2>技术选型</h2>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="6">
            <h4>后端技术</h4>
            <ul>
              <li>SpringBoot</li>
              <li>SpringCloud</li>
              <li>Nacos</li>
              <li>Sentinel</li>
              <li>Seata</li>
              <li>Minio</li>
              <li>...</li>
            </ul>
          </el-col>
          <el-col :span="6">
            <h4>前端技术</h4>
            <ul>
              <li>Vue</li>
              <li>Vuex</li>
              <li>Element-ui</li>
              <li>Axios</li>
              <li>Echarts</li>
              <li>Quill</li>
              <li>...</li>
            </ul>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
    <el-divider />
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="12" :lg="8">
        <el-card class="update-log">
          <div slot="header" class="clearfix">
            <span>联系信息</span>
          </div>
          <div class="body">
            <p>
              <i class="el-icon-s-promotion"></i> 官网：<el-link
                href="https://gitee.com/JavaLionLi/RuoYi-Cloud-Plus"
                target="_blank"
                >https://gitee.com/JavaLionLi/RuoYi-Cloud-Plus</el-link
              >
            </p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: "Index",
  data() {
    return {
      // 版本号
      version: "1.4.0",
    };
  },
  methods: {
    goTarget(href) {
      window.open(href, "_blank");
    },
  },
};
</script>

<style scoped lang="scss">
.home {
  blockquote {
    padding: 10px 20px;
    margin: 0 0 20px;
    font-size: 17.5px;
    border-left: 5px solid #eee;
  }
  hr {
    margin-top: 20px;
    margin-bottom: 20px;
    border: 0;
    border-top: 1px solid #eee;
  }
  .col-item {
    margin-bottom: 20px;
  }

  ul {
    padding: 0;
    margin: 0;
  }

  font-family: "open sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-size: 13px;
  color: #676a6c;
  overflow-x: hidden;

  ul {
    list-style-type: none;
  }

  h4 {
    margin-top: 0px;
  }

  h2 {
    margin-top: 10px;
    font-size: 26px;
    font-weight: 100;
  }

  p {
    margin-top: 10px;

    b {
      font-weight: 700;
    }
  }

  .update-log {
    ol {
      display: block;
      list-style-type: decimal;
      margin-block-start: 1em;
      margin-block-end: 1em;
      margin-inline-start: 0;
      margin-inline-end: 0;
      padding-inline-start: 40px;
    }
  }
}
</style>

