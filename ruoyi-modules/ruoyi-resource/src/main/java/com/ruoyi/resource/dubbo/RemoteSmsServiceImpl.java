package com.ruoyi.resource.dubbo;

import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.SpringUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.oss.entity.UploadResult;
import com.ruoyi.common.oss.factory.OssFactory;
import com.ruoyi.common.oss.service.IOssStrategy;
import com.ruoyi.common.sms.config.properties.SmsProperties;
import com.ruoyi.common.sms.core.SmsTemplate;
import com.ruoyi.common.sms.entity.SmsResult;
import com.ruoyi.resource.api.RemoteFileService;
import com.ruoyi.resource.api.RemoteSmsService;
import com.ruoyi.resource.api.domain.SysFile;
import com.ruoyi.resource.api.domain.SysSms;
import com.ruoyi.resource.domain.SysOss;
import com.ruoyi.resource.mapper.SysOssMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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

    private final SmsProperties smsProperties;

    /**
     * 发送短信
     *
     * @param phones     电话号(多个逗号分割)
     * @param templateId 模板id
     * @param param      模板对应参数
     */
    public SysSms send(String phones, String templateId, Map<String, String> param) throws ServiceException {
        if (smsProperties.getEnabled()) {
            R.fail("当前系统没有开启短信功能！");
        }
        SmsTemplate smsTemplate = SpringUtils.getBean(SmsTemplate.class);
        SmsResult smsResult = smsTemplate.send(phones, templateId, param);
        return BeanUtil.toBean(smsResult, SysSms.class);
    }

}
