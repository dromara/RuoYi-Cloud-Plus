FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/gateway
RUN mkdir -p /ruoyi/gateway/logs
RUN mkdir -p /ruoyi/gateway/temp

WORKDIR /ruoyi/gateway

EXPOSE 8080

ADD ./target/ruoyi-gateway.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
