package org.dromara.stream.consumer;

import lombok.extern.slf4j.Slf4j;
import org.dromara.stream.config.RabbitConfig;
import org.dromara.stream.config.RabbitTtlQueueConfig;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author xbhog
 * @date 2024年5月18日
 */
@Slf4j
@Component
public class RabbitConsumer {

    /**
     * 普通消息
     */
    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void listenQueue(Message message) {
        log.info("【消费者】Start consuming data：{}",new String(message.getBody()));
    }

    /**
     * 处理延迟队列消息
     */
    @RabbitListener(queues = RabbitTtlQueueConfig.DELAY_QUEUE_NAME)
    public void receiveDelayMessage(String message){
        log.info("【消费者】Received delayed message：{}",message);
    }

    /**
     * 处理死信队列消息
     * 当消息在延迟队列中未能被正确处理（例如因消费者逻辑错误、超时未ACK等原因）
     * 它会被自动转发到死信队列中，以便后续的特殊处理或重新尝试。
     */
    @RabbitListener(queues = RabbitTtlQueueConfig.DEAD_LETTER_QUEUE)
    public void receiveDeadMessage(String message){
        log.info("【消费者】Received dead message：{}",message);
    }
}
