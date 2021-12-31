package com.ruoyi.gateway.service;

import com.ruoyi.common.core.exception.CaptchaException;
import com.ruoyi.common.core.web.domain.AjaxResult;

import java.io.IOException;

/**
 * 验证码处理
 *
 * @author ruoyi
 */
public interface ValidateCodeService {
    /**
     * 生成验证码
     */
    AjaxResult createCapcha() throws IOException, CaptchaException;

    /**
     * 校验验证码
     */
    void checkCapcha(String key, String value) throws CaptchaException;
}
