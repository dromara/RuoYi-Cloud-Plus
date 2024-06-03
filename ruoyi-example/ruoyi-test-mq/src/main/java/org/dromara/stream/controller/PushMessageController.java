package org.dromara.stream.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.stream.mq.producer.kafkaMq.KafkaNormalProducer;
import org.dromara.stream.mq.producer.rabbitMq.DelayRabbitProducer;
import org.dromara.stream.mq.producer.rabbitMq.NormalRabbitProducer;
import org.dromara.stream.mq.producer.rocketMq.NormalRocketProducer;
import org.dromara.stream.mq.producer.rocketMq.TransactionRocketProducer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author xbhog
 */
@Slf4j
@RestController
@RequestMapping("push/message")
public class PushMessageController {

    @Resource
    private NormalRabbitProducer normalRabbitProducer;

    @Resource
    private DelayRabbitProducer delayRabbitProducer;

    @Resource
    private NormalRocketProducer normalRocketProducer;

    @Resource
    private TransactionRocketProducer transactionRocketProducer;

    @Resource
    private KafkaNormalProducer normalKafkaProducer;

    /**
     * rabbit普通消息的处理
     */
    @GetMapping("/rabbitMsg/sendNormal")
    public void sendMq() {
        normalRabbitProducer.sendMq("hello normal RabbitMsg");
    }

    /**
     * rabbit延迟队列类型：类似生产者
     */
    @GetMapping("/rabbitMsg/sendDelay")
    public void sendMessage() {
        delayRabbitProducer.sendDelayMessage("Hello ttl RabbitMsg");
    }

    /**
     * rockerMQ实例
     * 需要手动创建相关的Topic和group
     */
    @GetMapping("/rocketMq/send")
    public void sendRockerMq(){
        normalRocketProducer.sendMessage();
    }
    @GetMapping("/rocketMq/transactionMsg")
    public void sendRockerMqTransactionMsg(){
        transactionRocketProducer.sendTransactionMessage();
    }
    /**
     * kafkaSpringboot集成
     */
    @GetMapping("/kafkaMsg/send")
    public void sendKafkaMsg(){
        normalKafkaProducer.sendKafkaMsg();
    }
}
