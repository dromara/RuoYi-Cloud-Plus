package com.ruoyi.common.dubbo.filter;

import com.ruoyi.common.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.service.GenericService;

@Slf4j
@Activate(group = { CommonConstants.PROVIDER, CommonConstants.CONSUMER })
public class DubboRequestFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        //打印入参日志
        log.info("DUBBO - 服务入参: InterfaceName=[{}],MethodName=[{}],Parameter=[{}]", invocation.getInvoker().getInterface().getName(), invocation.getMethodName(), invocation.getArguments());
        //开始时间
        long startTime = System.currentTimeMillis();
        //执行接口调用逻辑
        Result result = invoker.invoke(invocation);
        //调用耗时
        long elapsed = System.currentTimeMillis() - startTime;
        //如果发生异常 则打印异常日志
        if (result.hasException() && invoker.getInterface().equals(GenericService.class)) {
            log.error("DUBBO - 执行异常: ", result.getException());
        } else {
            //打印响应日志
            log.info("DUBBO - 服务响应: InterfaceName=[{}],MethodName=[{}],SpendTime=[{}ms],Response=[{}]", invocation.getInvoker().getInterface().getName(), invocation.getMethodName(), elapsed, JsonUtils.toJsonString(new Object[]{result.getValue()}));
        }
        return result;
    }

}
