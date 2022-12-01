FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/resource/logs \
    /ruoyi/resource/temp \
    /ruoyi/skywalking/agent

WORKDIR /ruoyi/resource

ENV SERVER_PORT=9204

EXPOSE ${SERVER_PORT}

ADD ./target/ruoyi-resource.jar ./app.jar

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dserver.port=${SERVER_PORT}", \
#            "-Dskywalking.agent.service_name=ruoyi-resource", \
#            "-javaagent:/ruoyi/skywalking/agent/skywalking-agent.jar", \
            "-jar", "app.jar"]
