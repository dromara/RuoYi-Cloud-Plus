#FROM findepi/graalvm:java17-native
FROM openjdk:17.0.2-oraclelinux8

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/gateway/logs  \
    /ruoyi/gateway/temp  \
    /ruoyi/skywalking/agent

WORKDIR /ruoyi/gateway

ENV SERVER_PORT=8080 LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""

EXPOSE ${SERVER_PORT}

ADD ./target/ruoyi-gateway.jar ./app.jar

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${SERVER_PORT} \
           #-Dskywalking.agent.service_name=ruoyi-gateway \
           #-javaagent:/ruoyi/skywalking/agent/skywalking-agent.jar \
           -jar app.jar \
           -XX:+HeapDumpOnOutOfMemoryError -Xlog:gc*,:time,tags,level -XX:+UseZGC ${JAVA_OPTS}
