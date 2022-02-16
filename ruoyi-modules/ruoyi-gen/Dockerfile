FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/gen
RUN mkdir -p /ruoyi/gen/logs

WORKDIR /ruoyi/gen

EXPOSE 9202

ADD ./target/ruoyi-gen.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
