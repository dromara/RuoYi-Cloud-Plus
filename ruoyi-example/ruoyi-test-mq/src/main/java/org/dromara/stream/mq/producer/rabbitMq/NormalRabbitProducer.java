package org.dromara.stream.mq.producer.rabbitMq;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.stream.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * @author xbhog
 */
@Slf4j
@Component
public class NormalRabbitProducer {

    @Resource
    RabbitTemplate rabbitTemplate;


    public void sendMq(String message) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, RabbitConfig.ROUTING_KEY, message);
        log.info("【生产者】Message send: " + message);
    }
}
