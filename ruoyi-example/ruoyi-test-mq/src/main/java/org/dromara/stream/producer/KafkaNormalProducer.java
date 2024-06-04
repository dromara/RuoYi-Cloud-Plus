package org.dromara.stream.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * @author xbhog
 * @date 2024/05/19 18:02
 **/
@Slf4j
@Component
public class KafkaNormalProducer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendKafkaMsg() {
        kafkaTemplate.send("test-topic", "hello", "kafkaTest");
    }
}
