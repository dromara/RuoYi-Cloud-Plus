package org.dromara.common.mybatis.filter;

import org.apache.dubbo.rpc.*;
import org.dromara.common.mybatis.helper.DataPermissionHelper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class DubboDataPermissionFilterTest {
    @Test
    void attachesPermissionContextBeforeInvokingTheConsumer() {
        var calls = mock(Invoker.class);
        var invocation = mock(Invocation.class);
        var context = mock(RpcServiceContext.class);
        var permissions = Map.<String, Object>of("dept", 7L);
        var result = mock(Result.class);
        when(calls.invoke(invocation)).thenReturn(result);
        try (var rpc = mockStatic(RpcContext.class); var permission = mockStatic(DataPermissionHelper.class)) {
            rpc.when(RpcContext::getServiceContext).thenReturn(context);
            permission.when(DataPermissionHelper::getContext).thenReturn(permissions);
            assertSame(result, new DubboDataPermissionFilter().invoke(calls, invocation));
            var ordered = inOrder(context, calls);
            ordered.verify(context).setObjectAttachment(DataPermissionHelper.DATA_PERMISSION_KEY, permissions);
            ordered.verify(calls).invoke(invocation);
        }
    }
}
