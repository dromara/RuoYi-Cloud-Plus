FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/gateway
RUN mkdir -p /ruoyi/gateway/logs
RUN mkdir -p /ruoyi/gateway/temp

WORKDIR /ruoyi/gateway

ENV SERVER_PORT=8080

EXPOSE ${SERVER_PORT}

ADD ./target/ruoyi-gateway.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dserver.port=${SERVER_PORT}", "-jar", "app.jar"]
