FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/auth
RUN mkdir -p /ruoyi/auth/logs
RUN mkdir -p /ruoyi/auth/temp

WORKDIR /ruoyi/auth

ENV SERVER_PORT=9200

EXPOSE ${SERVER_PORT}

ADD ./target/ruoyi-auth.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dserver.port=${SERVER_PORT}", "-jar", "app.jar"]
