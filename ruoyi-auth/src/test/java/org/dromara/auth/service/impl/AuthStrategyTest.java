package org.dromara.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.extra.spring.SpringUtil;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthWechatMiniProgramRequest;
import org.dromara.auth.properties.CaptchaProperties;
import org.dromara.auth.service.IAuthStrategy;
import org.dromara.auth.service.SysLoginService;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.enums.LoginType;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.social.config.properties.SocialProperties;
import org.dromara.common.social.utils.SocialUtils;
import org.dromara.system.api.RemoteSocialService;
import org.dromara.system.api.RemoteUserService;
import org.dromara.system.api.domain.vo.RemoteClientVo;
import org.dromara.system.api.domain.vo.RemoteSocialVo;
import org.dromara.system.api.model.LoginUser;
import org.dromara.system.api.model.XcxLoginUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthStrategyTest {

    private final SysLoginService login = mock(SysLoginService.class);
    private final RemoteUserService users = mock(RemoteUserService.class);
    private final RemoteClientVo client = new RemoteClientVo();

    AuthStrategyTest() {
        client.setClientId("portal");
        client.setClientKey("web");
        client.setDeviceType("browser");
        client.setTimeout(600L);
        client.setActiveTimeout(120L);
        client.setAccessPath("/system/**");
    }

    @BeforeAll
    static void initializeUtilities() {
        try (var beans = mockStatic(SpringUtil.class)) {
            beans.when(() -> SpringUtil.getBean(JsonMapper.class)).thenReturn(JsonMapper.builder().build());
            beans.when(() -> SpringUtil.getBean(RedissonClient.class)).thenReturn(mock(RedissonClient.class));
            beans.when(() -> SpringUtil.getBean(jakarta.validation.Validator.class))
                .thenReturn(jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator());
            beans.when(() -> SpringUtil.getBean(org.dromara.common.social.utils.AuthRedisStateCache.class))
                .thenReturn(mock(org.dromara.common.social.utils.AuthRedisStateCache.class));
            JsonUtils.getJsonMapper();
            assertDoesNotThrow(() -> Class.forName(SocialUtils.class.getName()));
            RedisUtils.getClient();
            org.dromara.common.core.utils.ValidatorUtils.validate(new Object());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"password", "sms", "email"})
    void verifiesCredentialsAndIssuesTokenUsingCanonicalAccount(String kind) {
        var user = new LoginUser();
        user.setUsername("alice");
        user.setPassword(BCrypt.hashpw("example-password"));
        IAuthStrategy strategy;
        String fields;
        String cacheKey;
        LoginType type;
        switch (kind) {
            case "password" -> {
                var captcha = new CaptchaProperties();
                captcha.setEnabled(true);
                strategy = new PasswordAuthStrategy(captcha, login);
                when(users.getUserInfo("alice")).thenReturn(user);
                fields = "\"username\":\"alice\",\"password\":\"example-password\",\"uuid\":\"session\",\"code\":\"1234\"";
                cacheKey = "session";
                type = LoginType.PASSWORD;
            }
            case "sms" -> {
                strategy = new SmsAuthStrategy(login);
                when(users.getUserInfoByPhoneNumber("15500000001")).thenReturn(user);
                fields = "\"phoneNumber\":\"15500000001\",\"smsCode\":\"1234\"";
                cacheKey = "15500000001";
                type = LoginType.SMS;
            }
            default -> {
                strategy = new EmailAuthStrategy(login);
                when(users.getUserInfoByEmail("alice@example.org")).thenReturn(user);
                fields = "\"email\":\"alice@example.org\",\"emailCode\":\"1234\"";
                cacheKey = "alice@example.org";
                type = LoginType.EMAIL;
            }
        }
        ReflectionTestUtils.setField(strategy, "remoteUserService", users);
        doAnswer(call -> {
            Supplier<Boolean> failed = call.getArgument(2);
            assertFalse(failed.get());
            return null;
        }).when(login).checkLogin(eq(type), eq("alice"), any());
        try (var redis = mockStatic(RedisUtils.class); var sessions = mockStatic(LoginHelper.class);
             var tokens = mockStatic(StpUtil.class)) {
            redis.when(() -> RedisUtils.getCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + cacheKey)).thenReturn("1234");
            tokens.when(StpUtil::getTokenValue).thenReturn("session");
            tokens.when(StpUtil::getTokenTimeout).thenReturn(600L);

            var result = strategy.login(body(kind, fields), client);

            assertEquals("session", result.getAccessToken());
            assertEquals("portal", result.getClientId());
            assertEquals(600L, result.getExpireIn());
            assertEquals("web", user.getClientKey());
            sessions.verify(() -> LoginHelper.login(same(user), argThat((SaLoginParameter parameter) ->
                parameter.getTimeout() == 600 && "/system/**".equals(parameter.getExtra(LoginHelper.CLIENT_ACCESS_PATH_KEY)))));
            verify(login).checkLogin(eq(type), eq("alice"), any());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void socialLoginRequiresBoundAccount(boolean bound) {
        var properties = new SocialProperties();
        var bindings = mock(RemoteSocialService.class);
        var strategy = new SocialAuthStrategy(properties);
        ReflectionTestUtils.setField(strategy, "remoteSocialService", bindings);
        ReflectionTestUtils.setField(strategy, "remoteUserService", users);
        var binding = new RemoteSocialVo();
        binding.setUserId(42L);
        when(bindings.selectByAuthId("GITHUB42")).thenReturn(bound ? List.of(binding) : List.of());
        var user = new LoginUser();
        when(users.getUserInfo(42L)).thenReturn(user);
        var providerUser = AuthUser.builder().uuid("42").source("GITHUB").build();
        try (var social = mockStatic(SocialUtils.class); var sessions = mockStatic(LoginHelper.class);
             var tokens = mockStatic(StpUtil.class)) {
            social.when(() -> SocialUtils.loginAuth("github", "code", "state", properties))
                .thenReturn(AuthResponse.<AuthUser>builder().code(2000).data(providerUser).build());
            tokens.when(StpUtil::getTokenValue).thenReturn("social-token");
            String input = body("social", "\"source\":\"github\",\"socialCode\":\"code\",\"socialState\":\"state\"");
            if (bound) {
                assertEquals("social-token", strategy.login(input, client).getAccessToken());
                sessions.verify(() -> LoginHelper.login(same(user), any(SaLoginParameter.class)));
            } else {
                assertThrows(ServiceException.class, () -> strategy.login(input, client));
                verifyNoInteractions(users);
                sessions.verifyNoInteractions();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void miniProgramLoginUsesOpenIdOnlyAfterProviderAuthorization(boolean authorized) {
        var strategy = new XcxAuthStrategy(login);
        ReflectionTestUtils.setField(strategy, "remoteUserService", users);
        var providerUser = AuthUser.builder().token(AuthToken.builder().openId("open-42").build()).build();
        var user = new XcxLoginUser();
        when(users.getUserInfoByOpenid("open-42")).thenReturn(user);
        try (var providers = mockConstruction(AuthWechatMiniProgramRequest.class, (provider, context) ->
            when(provider.login(any())).thenReturn(AuthResponse.<AuthUser>builder()
                .code(authorized ? 2000 : 5000).data(providerUser).build()));
             var sessions = mockStatic(LoginHelper.class); var tokens = mockStatic(StpUtil.class)) {
            tokens.when(StpUtil::getTokenValue).thenReturn("mini-token");
            String input = body("xcx", "\"appid\":\"example-app\",\"xcxCode\":\"code\"");
            if (authorized) {
                var result = strategy.login(input, client);
                assertEquals("open-42", result.getOpenid());
                assertEquals("mini-token", result.getAccessToken());
                sessions.verify(() -> LoginHelper.login(same(user), any(SaLoginParameter.class)));
            } else {
                assertThrows(ServiceException.class, () -> strategy.login(input, client));
                verifyNoInteractions(users);
                sessions.verifyNoInteractions();
            }
            assertEquals(1, providers.constructed().size());
        }
    }

    private static String body(String grant, String fields) {
        return "{\"clientId\":\"portal\",\"grantType\":\"" + grant + "\"," + fields + "}";
    }
}
