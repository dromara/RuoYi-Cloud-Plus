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
    void publishMessage(Long sessionKey, String message);

    /**
     * 发布订阅的消息(群发)
     *
     * @param message 消息内容
     */
    void publishAll(String message);
}
