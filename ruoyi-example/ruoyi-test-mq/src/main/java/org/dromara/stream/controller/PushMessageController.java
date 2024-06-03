package org.dromara.stream.controller;

import lombok.extern.slf4j.Slf4j;
import org.dromara.stream.producer.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author xbhog
 */
@Slf4j
@RestController
@RequestMapping
public class PushMessageController {

    @Autowired
    private NormalRabbitProducer normalRabbitProducer;
    @Autowired
    private DelayRabbitProducer delayRabbitProducer;
    @Autowired
    private NormalRocketProducer normalRocketProducer;
    @Autowired
    private TransactionRocketProducer transactionRocketProducer;
    @Autowired
    private KafkaNormalProducer normalKafkaProducer;

    /**
     * rabbitmq 普通消息
     */
    @GetMapping("/rabbit/send")
    public void rabbitSend() {
        normalRabbitProducer.send("hello normal RabbitMsg");
    }

    /**
     * rabbitmq 延迟队列消息
     */
    @GetMapping("/rabbit/sendDelay")
    public void rabbitSendDelay(long delay) {
        delayRabbitProducer.sendDelayMessage("Hello ttl RabbitMsg", delay);
    }

    /**
     * rocketmq 发送消息
     * 需要手动创建相关的Topic和group
     */
    @GetMapping("/rocket/send")
    public void rocketSend(){
        normalRocketProducer.sendMessage();
    }

    /**
     * rocketmq 事务消息
     */
    @GetMapping("/rocket/transaction")
    public void rocketTransaction(){
        transactionRocketProducer.sendTransactionMessage();
    }

    /**
     * kafka 发送消息
     */
    @GetMapping("/kafka/send")
    public void kafkaSend(){
        normalKafkaProducer.sendKafkaMsg();
    }
}
