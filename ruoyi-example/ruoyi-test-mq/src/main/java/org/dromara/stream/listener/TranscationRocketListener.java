package org.dromara.stream.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * @author xbhog
 * @date 2024/06/01 17:05
 **/
@Slf4j
@Component
@RocketMQTransactionListener
public class TranscationRocketListener implements RocketMQLocalTransactionListener {

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object o) {
        log.info("执行本地事务");
        String tag = String.valueOf(message.getHeaders().get("rocketmq_TAGS"));
        if ("TAG-1".equals(tag)) {
            //这里只讲TAGA消息提交，状态为可执行
            log.info("【监听器】这里是校验TAG-1;提交状态:COMMIT");
            return RocketMQLocalTransactionState.COMMIT;
        } else if ("TAG-2".equals(tag)) {
            log.info("【监听器】这里是校验TAG-2;提交状态:ROLLBACK");
            return RocketMQLocalTransactionState.ROLLBACK;
        } else if ("TAG-3".equals(tag)) {
            log.info("【监听器】这里是校验TAG-3;提交状态:UNKNOWN");
            return RocketMQLocalTransactionState.UNKNOWN;
        }
        log.info("=========【监听器】提交状态:UNKNOWN");
        return RocketMQLocalTransactionState.UNKNOWN;
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message message) {
        log.info("【监听器】检查本地交易===>{}", message);
        return RocketMQLocalTransactionState.COMMIT;
    }

}
