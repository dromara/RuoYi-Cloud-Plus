package org.dromara.auth.listener;

import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.auth.event.UserLoginSuccessEvent;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.ip.AddressUtils;
import org.dromara.common.log.event.LoginInfoEvent;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.RemoteUserService;
import org.dromara.system.api.domain.SysUserOnline;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserLoginSuccessListenerTest {

    @BeforeAll
    static void initializeSpringBackedUtilities() {
        try (var beans = mockStatic(SpringUtil.class)) {
            beans.when(() -> SpringUtil.getBean(MessageSource.class)).thenReturn(mock(MessageSource.class));
            beans.when(() -> SpringUtil.getBean(RedissonClient.class)).thenReturn(mock(RedissonClient.class));
            MessageUtils.message("test.initialize");
            RedisUtils.getClient();
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 1200})
    void storesOnlineSessionAndUpdatesLoginAudit(long timeout) {
        var listener = new UserLoginSuccessListener();
        var users = mock(RemoteUserService.class);
        var events = mock(ApplicationContext.class);
        ReflectionTestUtils.setField(listener, "remoteUserService", users);
        var parameters = new SaLoginParameter();
        parameters.setTimeout(timeout);
        parameters.setDeviceType("web");
        parameters.setExtra(LoginHelper.USER_KEY, 42L);
        parameters.setExtra(LoginHelper.USER_NAME_KEY, "alice");
        parameters.setExtra(LoginHelper.CLIENT_KEY, "desktop");
        parameters.setExtra(LoginHelper.DEPT_NAME_KEY, "Support");
        var request = new MockHttpServletRequest();
        String ip = "192.0.2.10";
        if (timeout > 0) {
            request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/123.0.0.0 Safari/537.36");
        }
        try (var servlet = mockStatic(ServletUtils.class); var locations = mockStatic(AddressUtils.class);
             var redis = mockStatic(RedisUtils.class); var spring = mockStatic(SpringUtils.class);
             var messages = mockStatic(MessageUtils.class)) {
            servlet.when(ServletUtils::getRequest).thenReturn(request);
            servlet.when(ServletUtils::getClientIP).thenReturn(ip);
            locations.when(() -> AddressUtils.getRealAddressByIP(ip)).thenReturn("office");
            spring.when(SpringUtils::context).thenReturn(events);
            messages.when(() -> MessageUtils.message("user.login.success")).thenReturn("signed in");

            listener.handleLoginSuccess(new UserLoginSuccessEvent(42L, "session-token", parameters));

            String key = CacheNames.ONLINE_TOKEN_KEY + "session-token";
            var session = ArgumentCaptor.forClass(SysUserOnline.class);
            if (timeout < 0) {
                redis.verify(() -> RedisUtils.setCacheObject(eq(key), session.capture()));
                assertNull(session.getValue().getBrowser());
            } else {
                redis.verify(() -> RedisUtils.setCacheObject(eq(key), session.capture(), eq(Duration.ofSeconds(timeout))));
                assertEquals("Chrome", session.getValue().getBrowser());
            }
            var online = session.getValue();
            assertEquals("alice", online.getUserName());
            assertEquals("session-token", online.getTokenId());
            assertEquals(ip, online.getIpaddr());
            assertEquals("office", online.getLoginLocation());
            assertEquals("desktop", online.getClientKey());
            assertEquals("web", online.getDeviceType());
            assertEquals("Support", online.getDeptName());
            redis.verifyNoMoreInteractions();
            var audit = ArgumentCaptor.forClass(LoginInfoEvent.class);
            verify(events).publishEvent(audit.capture());
            assertEquals("alice", audit.getValue().getUsername());
            assertEquals(Constants.LOGIN_SUCCESS, audit.getValue().getStatus());
            assertEquals("signed in", audit.getValue().getMessage());
            verify(users).recordLoginInfo(42L, ip);
        }
    }
}
