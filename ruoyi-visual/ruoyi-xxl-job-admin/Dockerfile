FROM anapsix/alpine-java:8_server-jre_unlimited

MAINTAINER Lion Li

RUN mkdir -p /ruoyi/xxl-job-admin
RUN mkdir -p /ruoyi/xxl-job-admin/logs

WORKDIR /ruoyi/xxl-job-admin

ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 9900

ADD ./target/ruoyi-xxl-job-admin.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
