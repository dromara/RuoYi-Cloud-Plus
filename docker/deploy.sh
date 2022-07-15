#!/bin/bash

#使用说明，用来提示输入参数
usage() {
    echo "Usage: sh 执行脚本.sh [port|mount|monitor|base|start|stop|stopall|rm|rmiNoneTag]"
    exit 1
}

#开启所需端口(生产环境不推荐开启)
port(){
    # mysql 端口
    firewall-cmd --add-port=3306/tcp --permanent
    # redis 端口
    firewall-cmd --add-port=6379/tcp --permanent
    # minio api 端口
    firewall-cmd --add-port=9000/tcp --permanent
    # minio 控制台端口
    firewall-cmd --add-port=9001/tcp --permanent
    # 监控中心端口
    firewall-cmd --add-port=9100/tcp --permanent
    # 任务调度中心端口
    firewall-cmd --add-port=9900/tcp --permanent
    # nacos端口
    firewall-cmd --add-port=8848/tcp --permanent
    firewall-cmd --add-port=9848/tcp --permanent
    firewall-cmd --add-port=9849/tcp --permanent
    # sentinel端口
    firewall-cmd --add-port=8718/tcp --permanent
    # seata端口
    firewall-cmd --add-port=7091/tcp --permanent
    firewall-cmd --add-port=8091/tcp --permanent
    # elk端口
    firewall-cmd --add-port=9200/tcp --permanent
    firewall-cmd --add-port=5601/tcp --permanent
    firewall-cmd --add-port=4560/tcp --permanent
    # kafka端口
    firewall-cmd --add-port=2181/tcp --permanent
    firewall-cmd --add-port=9092/tcp --permanent
    firewall-cmd --add-port=19092/tcp --permanent
    # rabbitmq端口
    firewall-cmd --add-port=5672/tcp --permanent
    firewall-cmd --add-port=15672/tcp --permanent
    # rocketmq端口
    firewall-cmd --add-port=9876/tcp --permanent
    firewall-cmd --add-port=19876/tcp --permanent
    firewall-cmd --add-port=10911/tcp --permanent
    # 重启防火墙
    service firewalld restart
}

##放置挂载文件
mount(){
    #挂载 nginx 配置文件
    if test ! -f "/docker/nginx/" ;then
        mkdir -p /docker/nginx/
        cp -r nginx/* /docker/nginx
    fi
    #挂载 redis 配置文件
    if test ! -f "/docker/redis/" ;then
        mkdir -p /docker/redis/
        cp -r redis/* /docker/redis
    fi
    #挂载 nacos 配置文件
    if test ! -f "/docker/nacos/" ;then
        mkdir -p /docker/nacos/
        cp -r nacos/* /docker/nacos
    fi
    #挂载 elk 文件
    if test ! -f "/docker/elk/" ;then
        mkdir -p /docker/elk/
        cp -r elk/* /docker/elk
    fi
    #挂载 kafka 文件
    if test ! -f "/docker/kafka/" ;then
        mkdir -p /docker/kafka/
        cp -r kafka/* /docker/kafka
    fi
    #挂载 rabbitmq 文件
    if test ! -f "/docker/rabbitmq/" ;then
        mkdir -p /docker/rabbitmq/
        cp -r rabbitmq/data /docker/rabbitmq
        cp -r rabbitmq/log /docker/rabbitmq
    fi
    #挂载 rocketmq 文件
    if test ! -f "/docker/rocketmq/" ;then
        mkdir -p /docker/rocketmq/
        cp -r rocketmq/* /docker/rocketmq
    fi
    chmod -R 777 /docker
}

#启动基础模块
base(){
    docker-compose up -d mysql nacos seata-server nginx-web redis minio
}

#启动监控模块
monitor(){
    docker-compose up -d ruoyi-monitor sentinel ruoyi-xxl-job-admin
}

#启动程序模块
start(){
    docker-compose up -d ruoyi-gateway ruoyi-auth ruoyi-system ruoyi-resource
}

#停止程序模块
stop(){
    docker-compose stop ruoyi-gateway ruoyi-auth ruoyi-system ruoyi-resource
}

#关闭所有模块
stopall(){
    docker-compose stop
}

#删除所有模块
rm(){
    docker-compose rm
}

#删除Tag为空的镜像
rmiNoneTag(){
    docker images|grep none|awk '{print $3}'|xargs docker rmi -f
}

#根据输入参数，选择执行对应方法，不输入则执行使用说明
case "$1" in
"port")
    port
;;
"mount")
    mount
;;
"base")
    base
;;
"monitor")
    monitor
;;
"start")
    start
;;
"stop")
    stop
;;
"stopall")
    stopall
;;
"rm")
    rm
;;
"rmiNoneTag")
    rmiNoneTag
;;
*)
    usage
;;
esac
