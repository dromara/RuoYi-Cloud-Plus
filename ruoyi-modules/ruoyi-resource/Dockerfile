FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/resource
RUN mkdir -p /ruoyi/resource/logs
RUN mkdir -p /ruoyi/resource/temp

WORKDIR /ruoyi/resource

ENV SERVER_PORT=9300

EXPOSE ${SERVER_PORT}

ADD ./target/ruoyi-resource.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dserver.port=${SERVER_PORT}", "-jar", "app.jar"]
