FROM findepi/graalvm:java17-native

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/auth/logs  \
    /ruoyi/auth/temp  \
    /ruoyi/skywalking/agent

WORKDIR /ruoyi/auth

ENV SERVER_PORT=9210

EXPOSE ${SERVER_PORT}

ADD ./target/ruoyi-auth.jar ./app.jar

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dserver.port=${SERVER_PORT}", \
#            "-Dskywalking.agent.service_name=ruoyi-auth", \
#            "-javaagent:/ruoyi/skywalking/agent/skywalking-agent.jar", \
            "-jar", "app.jar"]
