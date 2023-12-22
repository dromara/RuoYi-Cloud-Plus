#FROM findepi/graalvm:java17-native
FROM openjdk:17.0.2-oraclelinux8

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/auth/logs  \
    /ruoyi/auth/temp  \
    /ruoyi/skywalking/agent

WORKDIR /ruoyi/auth

ENV SERVER_PORT=9210 LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""

EXPOSE ${SERVER_PORT}

ADD ./target/ruoyi-auth.jar ./app.jar

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${SERVER_PORT} \
           #-Dskywalking.agent.service_name=ruoyi-auth \
           #-javaagent:/ruoyi/skywalking/agent/skywalking-agent.jar \
           -jar app.jar \
           -XX:+HeapDumpOnOutOfMemoryError -Xlog:gc*,:time,tags,level -XX:+UseZGC ${JAVA_OPTS}

