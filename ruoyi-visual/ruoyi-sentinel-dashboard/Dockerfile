FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/sentinel-dashboard
RUN mkdir -p /ruoyi/sentinel-dashboard/logs

WORKDIR /ruoyi/sentinel-dashboard

EXPOSE 8718

ADD ./target/ruoyi-sentinel-dashboard.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
