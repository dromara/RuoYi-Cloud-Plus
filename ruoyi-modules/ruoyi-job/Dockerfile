FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/job
RUN mkdir -p /ruoyi/job/logs
RUN mkdir -p /ruoyi/job/temp

WORKDIR /ruoyi/job

EXPOSE 9203

ADD ./target/ruoyi-job.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
