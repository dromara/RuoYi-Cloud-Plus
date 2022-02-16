FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/doc
RUN mkdir -p /ruoyi/doc/logs

WORKDIR /ruoyi/doc

EXPOSE 18000

ADD ./target/ruoyi-doc.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
