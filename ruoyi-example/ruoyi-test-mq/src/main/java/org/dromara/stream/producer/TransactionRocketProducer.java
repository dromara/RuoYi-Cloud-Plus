package org.dromara.stream.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @author xbhog
 * @date 2024/06/01 16:54
 **/
@Slf4j
@Component
public class TransactionRocketProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void sendTransactionMessage() {
        List<String> tags = Arrays.asList("TAG-1", "TAG-2", "TAG-3");
        for (int i = 0; i < 3; i++) {
            Message<String> message = MessageBuilder.withPayload("===>事务消息-" + i).build();
            // destination formats: `topicName:tags` message – message Message arg – ext arg
            TransactionSendResult res = rocketMQTemplate.sendMessageInTransaction("transaction-topic:" + tags.get(i), message, i + 1);
            if (res.getLocalTransactionState().equals(LocalTransactionState.COMMIT_MESSAGE) && res.getSendStatus().equals(SendStatus.SEND_OK)) {
                log.info("【生产者】事物消息发送成功；成功结果：{}", res);
            } else {
                log.info("【生产者】事务发送失败：失败原因：{}", res);
            }
        }
    }

}
