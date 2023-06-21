package com.ruoyi.resource.dubbo;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.JsonUtils;
import com.ruoyi.resource.api.RemoteSmsService;
import com.ruoyi.resource.api.domain.SysSms;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.dromara.sms4j.provider.enumerate.SupplierType;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;

/**
 * 短信服务
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DubboService
public class RemoteSmsServiceImpl implements RemoteSmsService {

    /**
     * 发送短信
     *
     * @param phones     电话号(多个逗号分割)
     * @param templateId 模板id
     * @param param      模板对应参数
     */
    public SysSms send(String phones, String templateId, LinkedHashMap<String, String> param) throws ServiceException {
        SmsBlend smsBlend = SmsFactory.createSmsBlend(SupplierType.ALIBABA);
        SmsResponse smsResponse = smsBlend.sendMessage(phones, templateId, param);
        SysSms sysSms = new SysSms();
        sysSms.setIsSuccess(smsResponse.isSuccess());
        sysSms.setMessage(smsResponse.getMessage());
        sysSms.setResponse(JsonUtils.toJsonString(smsResponse));
        return sysSms;
    }

}
