FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/resource
RUN mkdir -p /ruoyi/resource/logs
RUN mkdir -p /ruoyi/resource/temp

WORKDIR /ruoyi/resource

EXPOSE 9300

ADD ./target/ruoyi-resource.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
