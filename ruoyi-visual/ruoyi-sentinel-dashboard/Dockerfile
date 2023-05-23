FROM findepi/graalvm:java17-native

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/sentinel-dashboard/logs \
    /ruoyi/skywalking/agent

WORKDIR /ruoyi/sentinel-dashboard

EXPOSE 8718

ADD ./target/ruoyi-sentinel-dashboard.jar ./app.jar

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
#            "-Dskywalking.agent.service_name=ruoyi-sentinel-dashboard", \
#            "-javaagent:/ruoyi/skywalking/agent/skywalking-agent.jar", \
            "-jar", "app.jar"]
