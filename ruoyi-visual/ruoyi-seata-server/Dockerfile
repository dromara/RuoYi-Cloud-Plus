# 贝尔实验室 Spring 官方推荐镜像 JDK下载地址 https://bell-sw.com/pages/downloads/
FROM bellsoft/liberica-openjdk-debian:17.0.11-cds
#FROM bellsoft/liberica-openjdk-debian:21.0.3-cds
#FROM findepi/graalvm:java17-native

LABEL maintainer="Lion Li"

RUN mkdir -p /ruoyi/seata-server/logs \
    /ruoyi/skywalking/agent

WORKDIR /ruoyi/seata-server

ENV TZ=PRC LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS="" SEATA_IP="" SEATA_PORT=""
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 7091
EXPOSE 8091

ADD ./target/ruoyi-seata-server.jar ./app.jar

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom \
           #-Dskywalking.agent.service_name=ruoyi-seata-server", \
           #-Dskywalking.plugin.seata.server=true", \
           #-javaagent:/ruoyi/skywalking/agent/skywalking-agent.jar", \
           ${JAVA_OPTS} -jar app.jar
