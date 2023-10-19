package org.dromara.resource.api;

import org.dromara.common.core.exception.ServiceException;

/**
 * 邮件服务
 *
 * @author Lion Li
 */
public interface RemoteMailService {

    /**
     * 发送邮件
     *
     * @param to      接收人
     * @param subject 标题
     * @param text    内容
     */
    void send(String to, String subject, String text) throws ServiceException;

}
