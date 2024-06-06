package org.dromara.stream.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author xbhog
 * @date 2024/05/19 18:04
 **/
@Slf4j
@Component
public class KafkaNormalConsumer {

    //默认获取最后一条消息
    @KafkaListener(topics = "test-topic", groupId = "test-group-id")
    public void timiKafka(ConsumerRecord<String, String> record) {
        Object key = record.key();
        Object value = record.value();
        log.info("【消费者】received the message key {}，value：{}", key, value);
    }

}
