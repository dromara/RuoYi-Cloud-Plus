package org.dromara.resource.api;

/**
 * 消息服务
 *
 * @author Lion Li
 */
public interface RemoteMessageService {

    /**
     * 发送消息
     *
     * @param sessionKey session主键 一般为用户id
     * @param message    消息文本
     */
    void sendMessage(Long sessionKey, String message);

    void publishAll(String message);
}
