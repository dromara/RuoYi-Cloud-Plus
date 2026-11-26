package org.dromara.auth.service;

import cn.hutool.extra.spring.SpringUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import org.dromara.auth.form.RegisterBody;
import org.dromara.auth.properties.CaptchaProperties;
import org.dromara.auth.properties.UserPasswordProperties;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.enums.LoginType;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.core.exception.user.CaptchaException;
import org.dromara.common.core.exception.user.CaptchaExpireException;
import org.dromara.common.core.exception.user.UserException;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.log.event.LoginInfoEvent;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.RemoteUserService;
import org.dromara.system.api.domain.bo.RemoteUserBo;
import org.dromara.system.api.model.LoginUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginLifecycleTest {

    private static final String USERNAME = "alice";
    private static final String RETRY_KEY = CacheNames.PWD_ERR_CNT_KEY + USERNAME;

    private final CaptchaProperties captcha = new CaptchaProperties();
    private final RemoteUserService users = mock(RemoteUserService.class);
    private final ApplicationContext events = mock(ApplicationContext.class);
    private final SysLoginService service = new SysLoginService(captcha);

    @BeforeAll
    static void initializeSpringBackedUtilities() {
        try (var beans = mockStatic(SpringUtil.class)) {
            beans.when(() -> SpringUtil.getBean(MessageSource.class)).thenReturn(mock(MessageSource.class));
            beans.when(() -> SpringUtil.getBean(RedissonClient.class)).thenReturn(mock(RedissonClient.class));
            MessageUtils.message("test.initialize");
            RedisUtils.getClient();
        }
    }

    LoginLifecycleTest() {
        captcha.setEnabled(false);
        var passwordRules = new UserPasswordProperties();
        passwordRules.setMaxRetryCount(3);
        passwordRules.setLockTime(5);
        ReflectionTestUtils.setField(service, "userPasswordProperties", passwordRules);
        ReflectionTestUtils.setField(service, "remoteUserService", users);
    }

    @ParameterizedTest
    @CsvSource({"0,1", "2,3"})
    void failedCredentialsRecordTheNextRetryWithAnExpiry(int attempts, int nextAttempt) {
        try (var redis = mockStatic(RedisUtils.class); var spring = mockStatic(SpringUtils.class);
             var messages = mockStatic(MessageUtils.class)) {
            spring.when(SpringUtils::context).thenReturn(events);
            redis.when(() -> RedisUtils.getCacheObject(RETRY_KEY)).thenReturn(attempts == 0 ? null : attempts);
            String code = nextAttempt == 3 ? LoginType.PASSWORD.getRetryLimitExceed() : LoginType.PASSWORD.getRetryLimitCount();
            if (nextAttempt == 3) {
                messages.when(() -> MessageUtils.message(code, 3, 5)).thenReturn(code);
            } else {
                messages.when(() -> MessageUtils.message(code, nextAttempt)).thenReturn(code);
            }

            assertThrows(UserException.class, () -> service.checkLogin(LoginType.PASSWORD, USERNAME, () -> true));

            redis.verify(() -> RedisUtils.setCacheObject(RETRY_KEY, nextAttempt, Duration.ofMinutes(5)));
            verifyLoginEvent(Constants.LOGIN_FAIL, code);
        }
    }

    @Test
    void lockedAccountSkipsCredentialVerificationAndLeavesExpiryIntact() {
        @SuppressWarnings("unchecked")
        Supplier<Boolean> credentials = mock(Supplier.class);
        try (var redis = mockStatic(RedisUtils.class); var spring = mockStatic(SpringUtils.class);
             var messages = mockStatic(MessageUtils.class)) {
            spring.when(SpringUtils::context).thenReturn(events);
            redis.when(() -> RedisUtils.getCacheObject(RETRY_KEY)).thenReturn(3);

            assertThrows(UserException.class, () -> service.checkLogin(LoginType.PASSWORD, USERNAME, credentials));

            verifyNoInteractions(credentials);
            redis.verify(() -> RedisUtils.getCacheObject(RETRY_KEY));
            redis.verifyNoMoreInteractions();
            verify(events).publishEvent(any(LoginInfoEvent.class));
        }
    }

    @Test
    void successfulAuthenticationClearsRetryCount() {
        try (var redis = mockStatic(RedisUtils.class)) {
            redis.when(() -> RedisUtils.getCacheObject(RETRY_KEY)).thenReturn(1);
            service.checkLogin(LoginType.PASSWORD, USERNAME, () -> false);
            redis.verify(() -> RedisUtils.deleteObject(RETRY_KEY));
            verifyNoInteractions(events);
        }
    }

    @ParameterizedTest
    @CsvSource({"AbC,abc,accepted", "AbC,wrong,mismatch", ",abc,expired"})
    void consumesCaptchaForEveryOutcome(String cached, String provided, String outcome) {
        String key = GlobalConstants.CAPTCHA_CODE_KEY + "registration";
        try (var redis = mockStatic(RedisUtils.class); var spring = mockStatic(SpringUtils.class);
             var messages = mockStatic(MessageUtils.class)) {
            spring.when(SpringUtils::context).thenReturn(events);
            redis.when(() -> RedisUtils.getCacheObject(key)).thenReturn(cached);
            switch (outcome) {
                case "accepted" -> {
                    service.validateCaptcha(USERNAME, provided, "registration");
                    verifyNoInteractions(events);
                }
                case "mismatch" -> assertThrows(CaptchaException.class,
                    () -> service.validateCaptcha(USERNAME, provided, "registration"));
                case "expired" -> assertThrows(CaptchaExpireException.class,
                    () -> service.validateCaptcha(USERNAME, provided, "registration"));
            }
            redis.verify(() -> RedisUtils.deleteObject(key));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void registrationSendsHashedCredentialsAndAuditsOnlySuccess(boolean registered) {
        var request = new RegisterBody();
        request.setUsername(USERNAME);
        request.setPassword("example-password");
        request.setUserType(UserType.SYS_USER.getUserType());
        when(users.registerUserInfo(any())).thenReturn(registered);
        try (var spring = mockStatic(SpringUtils.class); var messages = mockStatic(MessageUtils.class)) {
            spring.when(SpringUtils::context).thenReturn(events);
            messages.when(() -> MessageUtils.message("user.register.success")).thenReturn("registered");
            if (registered) {
                service.register(request);
                verifyLoginEvent(Constants.REGISTER, "registered");
            } else {
                assertThrows(UserException.class, () -> service.register(request));
                verifyNoInteractions(events);
            }
        }
        var saved = ArgumentCaptor.forClass(RemoteUserBo.class);
        verify(users).registerUserInfo(saved.capture());
        assertEquals(USERNAME, saved.getValue().getUserName());
        assertTrue(BCrypt.checkpw(request.getPassword(), saved.getValue().getPassword()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void logoutAlwaysClearsTheSessionAndAuditsAnExistingUser(boolean loggedIn) {
        var user = new LoginUser();
        user.setUsername(USERNAME);
        try (var login = mockStatic(LoginHelper.class); var token = mockStatic(StpUtil.class);
             var spring = mockStatic(SpringUtils.class); var messages = mockStatic(MessageUtils.class)) {
            login.when(LoginHelper::getLoginUser).thenReturn(loggedIn ? user : null);
            spring.when(SpringUtils::context).thenReturn(events);
            messages.when(() -> MessageUtils.message("user.logout.success")).thenReturn("logged out");

            service.logout();

            token.verify(StpUtil::logout);
            if (loggedIn) {
                verifyLoginEvent(Constants.LOGOUT, "logged out");
            } else {
                verifyNoInteractions(events);
            }
        }
    }

    private void verifyLoginEvent(String status, String message) {
        var event = ArgumentCaptor.forClass(LoginInfoEvent.class);
        verify(events).publishEvent(event.capture());
        assertEquals(USERNAME, event.getValue().getUsername());
        assertEquals(status, event.getValue().getStatus());
        assertEquals(message, event.getValue().getMessage());
    }
}
