package org.dromara.stream.controller;

import org.dromara.common.core.domain.R;
import org.dromara.stream.mq.producer.DelayProducer;
import org.dromara.stream.mq.producer.LogStreamProducer;
import org.dromara.stream.mq.producer.TestStreamProducer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试mq
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/test-mq")
public class TestMqController {

    private final DelayProducer delayProducer;
    private final TestStreamProducer testStreamProducer;
    private final LogStreamProducer logStreamProducer;

    /**
     * 发送消息Rabbitmq
     *
     * @param msg 消息内容
     * @param delay 延时时间
     */
    @GetMapping("/sendRabbitmq")
    public R<Void> sendRabbitmq(String msg, Long delay) {
        delayProducer.sendMsg(msg, delay);
        return R.ok();
    }

    /**
     * 发送消息Rocketmq
     *
     * @param msg 消息内容
     */
    @GetMapping("/sendRocketmq")
    public R<Void> sendRocketmq(String msg) {
        testStreamProducer.streamTestMsg(msg);
        return R.ok();
    }

    /**
     * 发送消息Kafka
     *
     * @param msg 消息内容
     */
    @GetMapping("/sendKafka")
    public R<Void> sendKafka(String msg) {
        logStreamProducer.streamLogMsg(msg);
        return R.ok();
    }

}
