package org.dromara.common.security;

import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.util.SaResult;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.security.config.SecurityConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mockStatic;

/**
 * common-security 单元测试（微服务版：网关校验过滤器与 actuator 鉴权）
 *
 * @author Lion Li
 */
@DisplayName("common-security 功能单元测试")
class SecurityConfigurationTest {

    /**
     * 验证拦截器注册器会添加覆盖全部路径的 Sa-Token 路由拦截器。
     */
    @Test
    @DisplayName("注册 Sa-Token 路由拦截器")
    void shouldRegisterSaInterceptorForAllPaths() {
        SecurityConfiguration configuration = new SecurityConfiguration();
        InterceptorRegistry registry = new InterceptorRegistry();

        configuration.addInterceptors(registry);

        @SuppressWarnings("unchecked")
        List<Object> registrations = (List<Object>) ReflectionTestUtils.invokeMethod(registry, "getInterceptors");
        assertEquals(1, registrations.size());
        Object interceptor = ReflectionTestUtils.getField(registrations.get(0), "interceptor");
        assertInstanceOf(SaInterceptor.class, interceptor);
    }

    /**
     * 验证网关转发校验过滤器覆盖全部路径并排除 actuator 端点，认证失败时返回未授权响应。
     */
    @Test
    @DisplayName("创建网关校验过滤器")
    void shouldCreateGatewayCheckFilter() {
        SecurityConfiguration configuration = new SecurityConfiguration();

        SaServletFilter filter = configuration.getSaServletFilter();

        assertEquals(List.of("/**"), filter.includeList);
        assertEquals(List.of("/actuator", "/actuator/**"), filter.excludeList);
        SaResult result = assertDoesNotThrow(() -> (SaResult) filter.error.run(new RuntimeException("认证失败")));
        assertEquals(HttpStatus.UNAUTHORIZED, result.getCode());
        assertEquals("认证失败，无法访问系统资源", result.getMsg());
    }

    /**
     * 验证 actuator 过滤器只拦截健康检查端点，账号密码来自 nacos 元数据配置，异常信息原样返回。
     */
    @Test
    @DisplayName("创建 actuator 鉴权过滤器")
    void shouldCreateActuatorFilterWithMetadataCredentials() {
        // getProperty 继承自 hutool SpringUtil，需在声明类上拦截静态方法
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getProperty("spring.cloud.nacos.discovery.metadata.username"))
                .thenReturn("nacos");
            spring.when(() -> SpringUtil.getProperty("spring.cloud.nacos.discovery.metadata.userpassword"))
                .thenReturn("secret");

            SecurityConfiguration configuration = new SecurityConfiguration();
            SaServletFilter filter = configuration.actuatorFilter();

            assertEquals(List.of("/actuator", "/actuator/**"), filter.includeList);
            assertEquals(List.of(), filter.excludeList);
            SaResult result = assertDoesNotThrow(() -> (SaResult) filter.error.run(new RuntimeException("boom")));
            assertEquals(HttpStatus.UNAUTHORIZED, result.getCode());
            assertEquals("boom", result.getMsg());
        }
    }

}
