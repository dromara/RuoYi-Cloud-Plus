package org.dromara.resource.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 消息服务
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteMessageServiceStub implements RemoteMessageService {

    private final RemoteMessageService remoteMessageService;

    /**
     * 发送消息
     *
     * @param sessionKey session主键 一般为用户id
     * @param message    消息文本
     */
    public void sendMessage(Long sessionKey, String message) {
        try {
            remoteMessageService.sendMessage(sessionKey, message);
        } catch (Exception e) {
            log.warn("websocket 功能未开启或服务未找到");
        }
    }

    public void publishAll(String message) {
        try {
            remoteMessageService.publishAll(message);
        } catch (Exception e) {
            log.warn("websocket 功能未开启或服务未找到");
        }
    }
}
