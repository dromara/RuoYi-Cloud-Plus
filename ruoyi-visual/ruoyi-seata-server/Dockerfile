#FROM findepi/graalvm:java17-native
FROM openjdk:17.0.2-oraclelinux8

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/seata-server/logs \
    /ruoyi/skywalking/agent

WORKDIR /ruoyi/seata-server

ENV TZ=PRC LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 7091
EXPOSE 8091

ADD ./target/ruoyi-seata-server.jar ./app.jar

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom \
           #-Dskywalking.agent.service_name=ruoyi-seata-server", \
           #-Dskywalking.plugin.seata.server=true", \
           #-javaagent:/ruoyi/skywalking/agent/skywalking-agent.jar", \
           -jar app.jar ${JAVA_OPTS}
