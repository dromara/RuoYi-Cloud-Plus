#FROM findepi/graalvm:java17-native
FROM openjdk:17.0.2-oraclelinux8

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/sentinel-dashboard/logs \
    /ruoyi/skywalking/agent

WORKDIR /ruoyi/sentinel-dashboard

ENV LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""

EXPOSE 8718

ADD ./target/ruoyi-sentinel-dashboard.jar ./app.jar

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom \
           #-Dskywalking.agent.service_name=ruoyi-sentinel-dashboard \
           #-javaagent:/ruoyi/skywalking/agent/skywalking-agent.jar \
           -jar app.jar ${JAVA_OPTS}
