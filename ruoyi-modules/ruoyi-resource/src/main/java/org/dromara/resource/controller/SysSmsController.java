package org.dromara.resource.controller;


import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.web.core.BaseController;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.sms.config.properties.SmsProperties;
import org.dromara.common.sms.core.SmsTemplate;
import org.dromara.common.sms.entity.SmsResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 短信功能
 *
 * @author Lion Li
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/sms")
public class SysSmsController extends BaseController {

    private final SmsProperties smsProperties;

    /**
     * 短信验证码
     *
     * @param phonenumber 用户手机号
     */
    @GetMapping("/code")
    public R<Void> smsCaptcha(@NotBlank(message = "{user.phonenumber.not.blank}") String phonenumber) {
        if (!smsProperties.getEnabled()) {
            return R.fail("当前系统没有开启短信功能！");
        }
        String key = GlobalConstants.CAPTCHA_CODE_KEY + phonenumber;
        String code = RandomUtil.randomNumbers(4);
        RedisUtils.setCacheObject(key, code, Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION));
        // 验证码模板id 自行处理 (查数据库或写死均可)
        String templateId = "";
        Map<String, String> map = new HashMap<>(1);
        map.put("code", code);
        SmsTemplate smsTemplate = SpringUtils.getBean(SmsTemplate.class);
        SmsResult result = smsTemplate.send(phonenumber, templateId, map);
        if (!result.getIsSuccess()) {
            log.error("验证码短信发送异常 => {}", result);
            return R.fail(result.getMessage());
        }
        return R.ok();
    }

}
