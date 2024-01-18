package org.dromara.resource.dubbo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.common.websocket.dto.WebSocketMessageDto;
import org.dromara.common.websocket.utils.WebSocketUtils;
import org.dromara.resource.api.RemoteMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息服务
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DubboService
public class RemoteMessageServiceImpl implements RemoteMessageService {

    /**
     * 发送消息
     *
     * @param sessionKey session主键 一般为用户id
     * @param message    消息文本
     */
    @Override
    public void publishMessage(Long sessionKey, String message) {
        WebSocketMessageDto dto = new WebSocketMessageDto();
        dto.setMessage(message);
        dto.setSessionKeys(List.of(sessionKey));
        WebSocketUtils.publishMessage(dto);
    }

    /**
     * 发布订阅的消息(群发)
     *
     * @param message 消息内容
     */
    @Override
    public void publishAll(String message) {
        WebSocketUtils.publishAll(message);
    }

}
