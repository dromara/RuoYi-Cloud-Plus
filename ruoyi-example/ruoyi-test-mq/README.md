# ruoyi-test-mq

## 模块说明

1. rabbitmq: 普通消息、延迟队列
2. rocketmq：普通消息、事务消息、延迟消息
3. kafka：普通消息


## 使用方式

rocketmq：

**注意：由于rocketmq并没有自动创建topic的功能, 所以需要进入到rockerMQ文件路径中执行**

创建普通消息的topic

```sh
sh mqadmin updateTopic -n <nameserver_address> -t <topic_name> -c <cluster_name> -a +message.type=NORMAL
```

```shell
bin/mqadmin updatetopic -n localhost:9876 -t test-topic -c DefaultCluster
```

创建事务消息的topic

```sh
sh mqadmin updateTopic -n <nameserver_address> -t <topic_name> -c <cluster_name> -a +message.type=TRANSACTION
```

```shell
bin/mqadmin updatetopic -n localhost:9876 -t transaction-topic -c DefaultCluster -a +message.type=TRANSACTION
```
