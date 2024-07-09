# 贝尔实验室 Spring 官方推荐镜像 JDK下载地址 https://bell-sw.com/pages/downloads/
FROM bellsoft/liberica-openjdk-debian:17.0.11-cds
#FROM bellsoft/liberica-openjdk-debian:21.0.3-cds
#FROM findepi/graalvm:java17-native

LABEL maintainer="Lion Li"

RUN mkdir -p /ruoyi/nacos

WORKDIR /ruoyi/nacos

EXPOSE 8848

ENV TZ=Asia/Shanghai LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS="-Xms512m -Xmx1024m"

ADD ./target/ruoyi-nacos.jar ./app.jar

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom ${JAVA_OPTS} -jar app.jar

