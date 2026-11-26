package org.dromara.gateway.filter;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.httpauth.basic.SaHttpBasicUtil;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.router.SaRouterStaff;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.gateway.config.properties.IgnoreWhiteProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthFilterTest {

    @ParameterizedTest
    @ValueSource(strings = {"authorized", "client", "path", "address", "session", "unrestricted"})
    void authorizationAppliesClientAndRequestRules(String scenario) {
        var ignore = new IgnoreWhiteProperties();
        ignore.setWhites(List.of());
        var request = new MockHttpServletRequest("GET", "/system/users");
        request.addHeader(LoginHelper.CLIENT_KEY, "web");
        var response = new MockHttpServletResponse();
        var filter = new AuthFilter(ignore).authSaServletFilter();
        try (var router = mockStatic(SaRouter.class); var tokens = mockStatic(StpUtil.class);
             var servlet = mockStatic(ServletUtils.class)) {
            router.when(() -> SaRouter.match("/**")).thenReturn(new SaRouterStaff().setHit(true));
            router.when(() -> SaRouter.isMatchCurrURI(List.of())).thenReturn(false);
            servlet.when(ServletUtils::getRequest).thenReturn(request);
            servlet.when(ServletUtils::getResponse).thenReturn(response);
            servlet.when(() -> ServletUtils.getClientIP(request)).thenReturn("192.0.2.10");
            tokens.when(() -> StpUtil.getExtra(LoginHelper.CLIENT_KEY)).thenReturn("client".equals(scenario) ? "other-client" : "web");
            tokens.when(StpUtil::getLoginType).thenReturn("login");
            if (!"unrestricted".equals(scenario)) {
                tokens.when(() -> StpUtil.getExtra(LoginHelper.CLIENT_ACCESS_PATH_KEY))
                    .thenReturn("path".equals(scenario) ? "/resource/**" : "/system/**");
                tokens.when(() -> StpUtil.getExtra(LoginHelper.CLIENT_IP_WHITELIST_KEY))
                    .thenReturn("address".equals(scenario) ? "192.0.2.20" : "192.0.2.10");
            }
            if ("session".equals(scenario)) {
                tokens.when(StpUtil::checkLogin).thenThrow(mock(NotLoginException.class));
            }
            switch (scenario) {
                case "authorized", "unrestricted" -> assertDoesNotThrow(() -> filter.auth.run(null));
                case "client", "session" -> {
                    var error = assertThrows(NotLoginException.class, () -> filter.auth.run(null));
                    assertEquals(401, ((SaResult) filter.error.run(error)).getCode());
                }
                default -> {
                    var error = assertThrows(NotPermissionException.class, () -> filter.auth.run(null));
                    assertEquals("认证失败，无法访问系统资源", ((SaResult) filter.error.run(error)).getMsg());
                }
            }
            tokens.verify(StpUtil::checkLogin);
        }
    }

    @Test
    void actuatorUsesConfiguredBasicCredentialsAndReportsFailure() {
        try (var properties = mockStatic(SpringUtil.class); var basic = mockStatic(SaHttpBasicUtil.class);
             var servlet = mockStatic(ServletUtils.class)) {
            properties.when(() -> SpringUtil.getProperty("spring.cloud.nacos.discovery.metadata.username")).thenReturn("monitor");
            properties.when(() -> SpringUtil.getProperty("spring.cloud.nacos.discovery.metadata.userpassword")).thenReturn("example");
            servlet.when(ServletUtils::getResponse).thenReturn(new MockHttpServletResponse());
            var filter = new AuthFilter(new IgnoreWhiteProperties()).getSaServletFilter();

            filter.auth.run(null);

            assertEquals(List.of("/actuator", "/actuator/**"), filter.includeList);
            basic.verify(() -> SaHttpBasicUtil.check("monitor:example"));
            assertEquals(401, ((SaResult) filter.error.run(new IllegalStateException("denied"))).getCode());
        }
    }
}
