package org.dromara.resource.dubbo;

import cn.hutool.core.util.ArrayUtil;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mail.utils.MailUtils;
import org.dromara.resource.api.RemoteMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * 邮件服务
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DubboService
public class RemoteMailServiceImpl implements RemoteMailService {

    /**
     * 发送邮件
     *
     * @param to      接收人
     * @param subject 标题
     * @param text    内容
     */
    public void send(String to, String subject, String text) throws ServiceException {
        MailUtils.sendText(to, subject, text);
    }

    /**
     * 发送邮件带附件
     *
     * @param to       接收人
     * @param subject  标题
     * @param text     内容
     * @param fileList 附件
     */
    public void sendWithAttachment(String to, String subject, String text, List<File> fileList) throws ServiceException {
        MailUtils.sendText(to, subject, text, ArrayUtil.toArray(fileList, File.class));
    }

}
