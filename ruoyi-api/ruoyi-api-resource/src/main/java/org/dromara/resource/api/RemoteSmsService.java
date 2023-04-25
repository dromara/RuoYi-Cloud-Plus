package org.dromara.resource.api;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.resource.api.domain.RemoteSms;

import java.util.Map;

/**
 * 短信服务
 *
 * @author Lion Li
 */
public interface RemoteSmsService {

    /**
     * 发送短信
     *
     * @param phones     电话号(多个逗号分割)
     * @param templateId 模板id
     * @param param      模板对应参数
     */
    RemoteSms send(String phones, String templateId, Map<String, String> param) throws ServiceException;

}
