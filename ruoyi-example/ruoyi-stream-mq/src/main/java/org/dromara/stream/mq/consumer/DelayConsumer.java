package org.dromara.stream.mq.consumer;


import org.dromara.stream.mq.TestMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
public class DelayConsumer {

    @Bean
    Consumer<TestMessaging> delay() {
        log.info("初始化订阅");
        return obj -> {
            log.info("消息接收成功：" + obj);
        };
    }
}
