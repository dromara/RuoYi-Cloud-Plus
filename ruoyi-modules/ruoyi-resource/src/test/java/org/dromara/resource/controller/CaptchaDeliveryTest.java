package org.dromara.resource.controller;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.mail.config.properties.MailProperties;
import org.dromara.common.mail.core.MailBuilder;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.redisson.api.RedissonClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CaptchaDeliveryTest {

    private static final String PHONE = "15500000001";
    private static final String EMAIL = "alice@example.org";
    private static final Duration TTL = Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION);

    @BeforeAll
    static void initializeRedisUtils() {
        try (var beans = mockStatic(SpringUtil.class)) {
            beans.when(() -> SpringUtil.getBean(RedissonClient.class)).thenReturn(mock(RedissonClient.class));
            RedisUtils.getClient();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void smsCachesCodeOnlyAfterProviderSuccess(boolean success) {
        var controller = new SysSmsController();
        var provider = mock(SmsBlend.class);
        var response = mock(SmsResponse.class);
        when(response.isSuccess()).thenReturn(success);
        when(provider.sendMessage(eq(PHONE), eq(""), any())).thenReturn(response);
        try (var factory = mockStatic(SmsFactory.class); var redis = mockStatic(RedisUtils.class)) {
            factory.when(() -> SmsFactory.getSmsBlend("tx1")).thenReturn(provider);
            assertFalse(R.isSuccess(controller.smsCaptcha("invalid")));
            var result = controller.smsCaptcha(PHONE);
            assertEquals(success, R.isSuccess(result));
            verify(provider).sendMessage(eq(PHONE), eq(""), argThat(params -> params.get("code").matches("[0-9]{4}")));
            if (success) {
                redis.verify(() -> RedisUtils.setCacheObject(eq(GlobalConstants.CAPTCHA_CODE_KEY + PHONE),
                    matches("[0-9]{4}"), eq(TTL)));
            } else {
                redis.verifyNoInteractions();
            }
        }
    }

    @Test
    void emailRejectsDisabledOrInvalidRequestsAndUsesRateLimitedEntry() {
        var properties = new MailProperties();
        var controller = new SysEmailController(properties);
        properties.setEnabled(false);
        assertFalse(R.isSuccess(controller.emailCode(EMAIL)));
        properties.setEnabled(true);
        assertFalse(R.isSuccess(controller.emailCode("invalid")));
        var proxy = mock(SysEmailController.class);
        try (var spring = mockStatic(SpringUtils.class)) {
            spring.when(() -> SpringUtils.getAopProxy(controller)).thenReturn(proxy);
            assertTrue(R.isSuccess(controller.emailCode(EMAIL)));
            verify(proxy).emailCodeImpl(EMAIL);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void emailCachesOnlySuccessfullySentCode(boolean success) {
        var controller = new SysEmailController(new MailProperties());
        var mail = mock(MailBuilder.class, RETURNS_SELF);
        if (!success) {
            doThrow(new IllegalStateException("mail unavailable")).when(mail).send();
        }
        try (var mailFactory = mockStatic(MailBuilder.class); var redis = mockStatic(RedisUtils.class)) {
            mailFactory.when(MailBuilder::of).thenReturn(mail);
            if (success) {
                controller.emailCodeImpl(EMAIL);
                redis.verify(() -> RedisUtils.setCacheObject(eq(GlobalConstants.CAPTCHA_CODE_KEY + EMAIL),
                    matches("[0-9]{4}"), eq(TTL)));
            } else {
                assertThrows(ServiceException.class, () -> controller.emailCodeImpl(EMAIL));
                redis.verifyNoInteractions();
            }
            verify(mail).to(EMAIL);
            verify(mail).send();
        }
    }
}
