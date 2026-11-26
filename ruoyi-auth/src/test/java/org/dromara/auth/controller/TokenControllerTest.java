package org.dromara.auth.controller;

import cn.hutool.extra.spring.SpringUtil;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import org.dromara.auth.domain.vo.LoginVo;
import org.dromara.auth.form.RegisterBody;
import org.dromara.auth.form.SocialLoginBody;
import org.dromara.auth.service.IAuthStrategy;
import org.dromara.auth.service.SysLoginService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.social.config.properties.SocialLoginConfigProperties;
import org.dromara.common.social.config.properties.SocialProperties;
import org.dromara.common.social.utils.SocialUtils;
import org.dromara.resource.api.RemoteMessageService;
import org.dromara.system.api.RemoteClientService;
import org.dromara.system.api.RemoteConfigService;
import org.dromara.system.api.RemoteSocialService;
import org.dromara.system.api.domain.vo.RemoteClientVo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenControllerTest {
    private final SocialProperties social = new SocialProperties();
    private final SysLoginService login = mock(SysLoginService.class);
    private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
    private final RemoteConfigService config = mock(RemoteConfigService.class);
    private final RemoteClientService clients = mock(RemoteClientService.class);
    private final RemoteSocialService bindings = mock(RemoteSocialService.class);
    private final RemoteMessageService messages = mock(RemoteMessageService.class);
    private final TokenController controller = new TokenController(social, login, scheduler, config, clients, bindings, messages);

    @BeforeAll
    static void initializeJson() {
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(JsonMapper.class)).thenReturn(JsonMapper.builder().build());
            spring.when(() -> SpringUtil.getBean(MessageSource.class)).thenReturn(mock(MessageSource.class));
            spring.when(() -> SpringUtil.getBean(jakarta.validation.Validator.class))
                .thenReturn(jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator());
            spring.when(() -> SpringUtil.getBean(org.dromara.common.social.utils.AuthRedisStateCache.class))
                .thenReturn(mock(org.dromara.common.social.utils.AuthRedisStateCache.class));
            JsonUtils.getJsonMapper();
            assertDoesNotThrow(() -> Class.forName(SocialUtils.class.getName()));
            MessageUtils.message("initialize");
            org.dromara.common.core.utils.ValidatorUtils.validate(new Object());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "unsupported", "disabled", "active"})
    void dispatchesOnlyAuthorizedClientAndSchedulesWelcome(String state) {
        String input = """
            {"clientId":"web","grantType":"password"}
            """;
        var client = new RemoteClientVo();
        client.setGrantType("unsupported".equals(state) ? "email" : "password");
        client.setStatus("disabled".equals(state) ? "1" : "0");
        when(clients.queryByClientId("web")).thenReturn("missing".equals(state) ? null : client);
        try (var strategies = mockStatic(IAuthStrategy.class); var account = mockStatic(LoginHelper.class)) {
            var expected = new LoginVo();
            strategies.when(() -> IAuthStrategy.login(input, client, "password")).thenReturn(expected);
            account.when(LoginHelper::getUserId).thenReturn(42L);
            var result = controller.login(input);
            if ("active".equals(state)) {
                assertSame(expected, result.getData());
                var task = ArgumentCaptor.forClass(Runnable.class);
                verify(scheduler).schedule(task.capture(), eq(5L), eq(TimeUnit.SECONDS));
                task.getValue().run();
                verify(messages).publishMessage(eq(List.of(42L)), contains("欢迎登录"));
            } else {
                assertFalse(R.isSuccess(result));
                strategies.verifyNoInteractions();
                verifyNoInteractions(scheduler, messages);
            }
        }
    }

    @Test
    void rejectsUnknownSocialPlatformAndReturnsAuthorizationUrlForConfiguredPlatform() {
        social.setType(Map.of("github", new SocialLoginConfigProperties()));
        assertFalse(R.isSuccess(controller.authBinding("unknown")));
        var provider = mock(AuthRequest.class);
        when(provider.authorize(anyString())).thenReturn("https://example.org/authorize");
        try (var utils = mockStatic(SocialUtils.class)) {
            utils.when(() -> SocialUtils.getAuthRequest("github", social)).thenReturn(provider);
            assertEquals("https://example.org/authorize", controller.authBinding("github").getData());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void appliesProviderCallbackAndRegistrationOutcomes(boolean success) {
        var request = new SocialLoginBody();
        request.setSource("github");
        request.setSocialCode("code");
        request.setSocialState("state");
        var user = AuthUser.builder().uuid("42").build();
        @SuppressWarnings("unchecked")
        AuthResponse<AuthUser> response = mock(AuthResponse.class);
        when(response.ok()).thenReturn(success);
        when(response.getData()).thenReturn(user);
        when(bindings.deleteWithValidById(9L)).thenReturn(success);
        when(config.selectRegisterEnabled()).thenReturn(success);
        var registration = new RegisterBody();
        try (var utils = mockStatic(SocialUtils.class)) {
            utils.when(() -> SocialUtils.loginAuth("github", "code", "state", social)).thenReturn(response);
            assertEquals(success, R.isSuccess(controller.socialCallback(request)));
        }
        assertEquals(success, R.isSuccess(controller.unlockSocial(9L)));
        assertEquals(success, R.isSuccess(controller.register(registration)));
        assertTrue(R.isSuccess(controller.logout()));
        verify(login, times(success ? 1 : 0)).socialRegister(user);
        verify(login, times(success ? 1 : 0)).register(registration);
        verify(login).logout();
    }
}
