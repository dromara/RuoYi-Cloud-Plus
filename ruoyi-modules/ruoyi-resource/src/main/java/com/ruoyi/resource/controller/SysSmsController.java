package com.ruoyi.resource.controller;


import cn.hutool.core.util.RandomUtil;
import com.ruoyi.common.core.constant.Constants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.utils.SpringUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.redis.utils.RedisUtils;
import com.ruoyi.common.sms.config.properties.SmsProperties;
import com.ruoyi.common.sms.core.SmsTemplate;
import com.ruoyi.common.sms.entity.SmsResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
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
@Api(value = "短信功能控制器", tags = {"短信功能管理"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/sms")
public class SysSmsController extends BaseController {

    private final SmsProperties smsProperties;

    /**
     * 短信验证码
     */
    @ApiOperation("短信验证码")
    @GetMapping("/code")
    public R<Void> smsCaptcha(@ApiParam("用户手机号")
                              @NotBlank(message = "{user.phonenumber.not.blank}")
                              String phonenumber) {
        if (smsProperties.getEnabled()) {
            R.fail("当前系统没有开启短信功能！");
        }
        String key = Constants.CAPTCHA_CODE_KEY + phonenumber;
        String code = RandomUtil.randomNumbers(4);
        RedisUtils.setCacheObject(key, code, Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION));
        // 验证码模板id 自行处理 (查数据库或写死均可)
        String templateId = "";
        Map<String, String> map = new HashMap<>(1);
        map.put("code", code);
        SmsTemplate smsTemplate = SpringUtils.getBean(SmsTemplate.class);
        SmsResult result = smsTemplate.send(phonenumber, templateId, map);
        if (!result.isSuccess()) {
            log.error("验证码短信发送异常 => {}", result);
            return R.fail(result.getMessage());
        }
        return R.ok();
    }

}
