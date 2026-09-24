package org.dromara.common.dubbo;

import cn.hutool.extra.spring.SpringUtil;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcServiceContext;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.dubbo.enumd.RequestLogEnum;
import org.dromara.common.dubbo.filter.DubboRequestFilter;
import org.dromara.common.dubbo.handler.DubboExceptionHandler;
import org.dromara.common.dubbo.properties.DubboCustomProperties;
import org.dromara.common.json.utils.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("common-dubbo 功能单元测试")
class DubboFunctionTest {

    @BeforeAll
    static void initJsonMapper() {
        // JsonUtils 静态依赖 Spring 容器获取 JsonMapper，先初始化最小容器并触发类加载
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(JsonMapper.class, () -> JsonMapper.builder().build());
        context.refresh();
        new SpringUtils().setApplicationContext(context);
        JsonUtils.getJsonMapper();
    }

    /**
     * 验证 RPC 异常被转换为统一的失败响应。
     */
    @Test
    @DisplayName("转换 RPC 异常为统一响应")
    void shouldConvertRpcExceptionToFailResponse() {
        R<Void> response = new DubboExceptionHandler().handleDubboException(new RpcException("timeout"));

        assertEquals(HttpStatus.ERROR, response.getCode());
        assertEquals("RPC异常，请联系管理员确认", response.getMsg());
    }

    /**
     * 验证自定义配置属性保存请求日志开关与级别，枚举覆盖三种日志级别。
     */
    @Test
    @DisplayName("保存自定义配置属性")
    void shouldStoreCustomProperties() {
        DubboCustomProperties properties = new DubboCustomProperties();
        properties.setRequestLog(true);
        properties.setLogLevel(RequestLogEnum.FULL);

        assertTrue(properties.getRequestLog());
        assertEquals(RequestLogEnum.FULL, properties.getLogLevel());
        assertArrayEquals(new RequestLogEnum[]{RequestLogEnum.INFO, RequestLogEnum.PARAM, RequestLogEnum.FULL},
            RequestLogEnum.values());
    }

    /**
     * 验证关闭请求日志时过滤器直接透传服务调用，不做任何包装。
     */
    @Test
    @DisplayName("关闭请求日志时直接透传调用")
    @SuppressWarnings("unchecked")
    void shouldSkipLoggingWhenRequestLogDisabled() {
        DubboCustomProperties properties = new DubboCustomProperties();
        properties.setRequestLog(false);
        Invoker<Object> invoker = mock(Invoker.class);
        Invocation invocation = mock(Invocation.class);
        Result result = mock(Result.class);
        when(invoker.invoke(invocation)).thenReturn(result);

        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(DubboCustomProperties.class)).thenReturn(properties);

            Result actual = new DubboRequestFilter().invoke(invoker, invocation);

            assertSame(result, actual);
            verify(invoker).invoke(invocation);
        }
    }

    /**
     * 验证开启请求日志后按 FULL 级别记录调用与响应，正常结果与异常结果均原样返回。
     */
    @Test
    @DisplayName("按 FULL 级别记录请求日志")
    @SuppressWarnings("unchecked")
    void shouldLogRequestWithFullLevelAndReturnResult() {
        DubboCustomProperties properties = new DubboCustomProperties();
        properties.setRequestLog(true);
        properties.setLogLevel(RequestLogEnum.FULL);

        Invoker<Object> invoker = mock(Invoker.class);
        when(invoker.getInterface()).thenReturn((Class) DubboFunctionTest.class);
        Invocation invocation = mock(Invocation.class);
        when(invocation.getInvoker()).thenReturn((Invoker) invoker);
        when(invocation.getMethodName()).thenReturn("echo");
        when(invocation.getArguments()).thenReturn(new Object[]{"arg"});

        Result success = mock(Result.class);
        when(success.hasException()).thenReturn(false);
        when(success.getValue()).thenReturn("ok");
        when(invoker.invoke(invocation)).thenReturn(success);

        RpcServiceContext serviceContext = mock(RpcServiceContext.class);
        when(serviceContext.isConsumerSide()).thenReturn(false);

        try (var spring = mockStatic(SpringUtil.class); var rpc = mockStatic(RpcContext.class)) {
            spring.when(() -> SpringUtil.getBean(DubboCustomProperties.class)).thenReturn(properties);
            rpc.when(RpcContext::getServiceContext).thenReturn(serviceContext);

            DubboRequestFilter filter = new DubboRequestFilter();
            assertSame(success, filter.invoke(invoker, invocation));

            // 异常结果且非泛化调用时走异常日志分支，仍原样返回
            Result failure = mock(Result.class);
            when(failure.hasException()).thenReturn(true);
            when(failure.getException()).thenReturn(new RpcException("boom"));
            when(invoker.invoke(invocation)).thenReturn(failure);
            assertSame(failure, filter.invoke(invoker, invocation));
        }
    }

}
