package org.dromara.resource.controller;


import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.common.mail.config.properties.MailProperties;
import org.dromara.common.mail.utils.MailUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;

/**
 * 邮件功能
 *
 * @author Lion Li
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/email")
public class SysEmailController extends BaseController {

    private final MailProperties mailProperties;

    /**
     * 邮箱验证码
     *
     * @param email 邮箱
     */
    @GetMapping("/code")
    public R<Void> emailCode(@NotBlank(message = "{user.email.not.blank}") String email) {
        if (!mailProperties.getEnabled()) {
            return R.fail("当前系统没有开启邮箱功能！");
        }
        String key = GlobalConstants.CAPTCHA_CODE_KEY + email;
        String code = RandomUtil.randomNumbers(4);
        RedisUtils.setCacheObject(key, code, Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION));
        try {
            MailUtils.sendText(email, "登录验证码", "您本次验证码为：" + code + "，有效性为" + Constants.CAPTCHA_EXPIRATION + "分钟，请尽快填写。");
        } catch (Exception e) {
            log.error("验证码短信发送异常 => {}", e.getMessage());
            return R.fail(e.getMessage());
        }
        return R.ok();
    }

}
