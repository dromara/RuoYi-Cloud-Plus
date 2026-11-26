package org.dromara.auth.listener;

import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.auth.event.UserLoginSuccessEvent;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserActionListenerTest {
    private final UserActionListener listener = new UserActionListener();

    @Test
    void loginPublishesTheSessionIdentityAndParameters() {
        var events = mock(ApplicationContext.class);
        var parameters = new SaLoginParameter();
        try (var spring = mockStatic(SpringUtils.class)) {
            spring.when(SpringUtils::context).thenReturn(events);
            listener.doLogin("login", 42L, "token", parameters);
            var captured = org.mockito.ArgumentCaptor.forClass(UserLoginSuccessEvent.class);
            verify(events).publishEvent(captured.capture());
            assertEquals(42L, captured.getValue().loginId());
            assertEquals("token", captured.getValue().tokenValue());
            assertSame(parameters, captured.getValue().loginParameter());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"logout", "kickout", "replaced"})
    void sessionTerminationRemovesTheOnlineRecord(String operation) {
        try (var beans = mockStatic(SpringUtil.class)) {
            beans.when(() -> SpringUtil.getBean(RedissonClient.class)).thenReturn(mock(RedissonClient.class));
            RedisUtils.getClient();
        }
        try (var redis = mockStatic(RedisUtils.class)) {
            switch (operation) {
                case "logout" -> listener.doLogout("login", 42L, "token");
                case "kickout" -> listener.doKickout("login", 42L, "token");
                case "replaced" -> listener.doReplaced("login", 42L, "token");
            }
            redis.verify(() -> RedisUtils.deleteObject(CacheNames.ONLINE_TOKEN_KEY + "token"));
            redis.verifyNoMoreInteractions();
        }
    }
}
