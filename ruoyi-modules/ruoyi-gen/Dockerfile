FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/gen
RUN mkdir -p /ruoyi/gen/logs

WORKDIR /ruoyi/gen

ENV SERVER_PORT=9202

EXPOSE ${SERVER_PORT}

ADD ./target/ruoyi-gen.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dserver.port=${SERVER_PORT}", "-jar", "app.jar"]
