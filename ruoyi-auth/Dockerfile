FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/auth
RUN mkdir -p /ruoyi/auth/logs
RUN mkdir -p /ruoyi/auth/temp

WORKDIR /ruoyi/auth

EXPOSE 9200

ADD ./target/ruoyi-auth.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
