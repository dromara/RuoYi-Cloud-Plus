# ruoyi-test-mq

## 模块说明

该模块基于需求【[将spring-cloud-stream改为普通spring的mq依赖用法】下修改编写；

原模块缺点：功能复杂学习成本高 大部分用户用不明白 功能封闭 特性无法使用 项目中基本不会有切换mq的事情发生；

现模块：集成基础的rabbit、rocketmq、kafka等主流的中间件，功能包含

1. rabbit: 普通消息、延迟队列
2. rocket：普通消息、事务消息
3. kafka：普通消息

后续可实现的：

1. kafka stream流的使用
2. rocket 顺序、异步、延时等

## 项目目录

```xml
├─src
│  └─main
│      ├─java
│      │  └─org
│      │      └─dromara
│      │          └─stream
│      │              │  RuoYiTestMqApplication.java
│      │              │  
│      │              ├─config
│      │              │      RabbitConfig.java 普通消息配置类
│      │              │      RabbitTtlQueueConfig.java  延迟队列配置类
│      │              │      
│      │              ├─controller  测试类
│      │              │      PushMessageController.java
│      │              │      
│      │              └─mq
│      │                  ├─consumer
│      │                  │  ├─kafkaMq
│      │                  │  │      KafkaNormalConsumer.java
│      │                  │  ├─rabbit
│      │                  │  │      ConsumerListener.java
│      │                  │  └─rocketmq
│      │                  │          NormalRocketConsumer.java
│      │                  │          TransactionRocketConsumer.java
│      │                  ├─listener
│      │                  │      TranscationRocketListener.java
│      │                  └─producer
│      │                      ├─kafkaMq
│      │                      │      KafkaNormalProducer.java
│      │                      ├─rabbitMq
│      │                      │      DelayRabbitProducer.java
│      │                      │      NormalRabbitProducer.java
│      │                      └─rocketMq
│      │  	                         NormalRocketProducer.java
│      │  	                         TransactionRocketProducer.java
│      │                                  
│      └─resources
│              application.yml  IP:Host根据实际情况替换
│              logback-plus.xml
```

## 使用方式

rocketmq：

**注意：需要进入到rockerMQ文件路径中执行**

创建普通消息的topic

```sh
sh mqadmin updateTopic -n <nameserver_address> -t <topic_name> -c <cluster_name> -a +message.type=NORMAL
```

```shell
bin/mqadmin updatetopic -n localhost:9876 -t TestTopic -c DefaultCluster
```

创建事务消息的topic

```sh
sh mqadmin updateTopic -n <nameserver_address> -t <topic_name> -c <cluster_name> -a +message.type=TRANSACTION
```

```shell
bin/mqadmin updatetopic -n localhost:9876 -t transaction_topic -c DefaultCluster -a +message.type=TRANSACTION
```

kafka:

```shell
kafka-topics.sh --create --topic <topic_name> --bootstrap-server <broker_list> --partitions <num_partitions> --replication-factor <replication_factor>
```

```shell
kafka-topics.sh --create --topic my_topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

## 验证方式

可通过`PushMessageController`实现`Restful`进行测试;