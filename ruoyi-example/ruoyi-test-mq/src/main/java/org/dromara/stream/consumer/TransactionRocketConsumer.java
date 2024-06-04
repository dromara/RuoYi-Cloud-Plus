package org.dromara.stream.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @author xbhog
 * @date 2024/06/01 16:54
 **/
@Slf4j
@Component
@RocketMQMessageListener(topic = "transaction-topic", consumerGroup = "transaction-group")
public class TransactionRocketConsumer  implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.info("【消费者】===>接收事务消息：{}",message);
    }

}
