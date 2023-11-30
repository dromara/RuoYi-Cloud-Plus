#FROM findepi/graalvm:java17-native
FROM openjdk:17.0.2-oraclelinux8

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/nacos

WORKDIR /ruoyi/nacos

EXPOSE 8848

ENV TZ=Asia/Shanghai LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS="-Xms512m -Xmx1024m"

ADD ./target/ruoyi-nacos.jar ./app.jar

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -jar app.jar ${JAVA_OPTS}

