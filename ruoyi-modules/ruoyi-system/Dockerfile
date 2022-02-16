FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/system
RUN mkdir -p /ruoyi/system/logs
RUN mkdir -p /ruoyi/system/temp

WORKDIR /ruoyi/system

EXPOSE 9201

ADD ./target/ruoyi-system.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
