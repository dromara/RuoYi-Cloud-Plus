package org.dromara.auth.controller;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.auth.properties.CaptchaProperties;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.redisson.api.RedissonClient;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CaptchaControllerTest {

    @BeforeAll
    static void initializeRedis() {
        try (var beans = mockStatic(SpringUtil.class)) {
            beans.when(() -> SpringUtil.getBean(RedissonClient.class)).thenReturn(mock(RedissonClient.class));
            RedisUtils.getClient();
        }
    }

    @Test
    void disabledCaptchaReturnsWithoutCaching() {
        var settings = new CaptchaProperties();
        settings.setEnabled(false);
        try (var redis = mockStatic(RedisUtils.class)) {
            assertEquals(false, new CaptchaController(settings).getCode().getData().getCaptchaEnabled());
            redis.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"math", "char"})
    void enabledCaptchaReturnsReadableImageAndCachesTheAnswer(String type) throws Exception {
        var settings = new CaptchaProperties();
        settings.setEnabled(true);
        settings.setType(type);
        settings.setCharLength(4);
        settings.setNumberLength(2);
        var controller = new CaptchaController(settings);
        try (var spring = mockStatic(SpringUtils.class); var redis = mockStatic(RedisUtils.class)) {
            spring.when(() -> SpringUtils.getAopProxy(controller)).thenReturn(controller);
            var result = controller.getCode().getData();
            assertNotNull(result.getUuid());
            var image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(result.getImg())));
            assertEquals(160, image.getWidth());
            assertEquals(60, image.getHeight());
            String answer = "math".equals(type) ? "-?[0-9]+" : "[A-Za-z0-9]{4}";
            redis.verify(() -> RedisUtils.setCacheObject(eq(GlobalConstants.CAPTCHA_CODE_KEY + result.getUuid()),
                matches(answer), eq(Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION))));
        }
    }
}
