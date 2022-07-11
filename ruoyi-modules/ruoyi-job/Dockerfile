FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/job
RUN mkdir -p /ruoyi/job/logs
RUN mkdir -p /ruoyi/job/temp

WORKDIR /ruoyi/job

ENV SERVER_PORT=9203

EXPOSE ${SERVER_PORT}

ADD ./target/ruoyi-job.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dserver.port=${SERVER_PORT}", "-jar", "app.jar"]
