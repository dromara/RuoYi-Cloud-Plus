package org.dromara.common.dubbo.filter;

import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.dubbo.enumd.RequestLogEnum;
import org.dromara.common.dubbo.properties.DubboCustomProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.service.GenericService;
import org.dromara.common.json.utils.JsonUtils;

/**
 * dubbo日志过滤器
 *
 * @author Lion Li
 */
@Slf4j
@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER}, order = Integer.MAX_VALUE)
public class DubboRequestFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        DubboCustomProperties properties = SpringUtils.getBean(DubboCustomProperties.class);
        if (!properties.getRequestLog()) {
            // 未开启则跳过日志逻辑
            return invoker.invoke(invocation);
        }
        String client = CommonConstants.PROVIDER;
        if (RpcContext.getServiceContext().isConsumerSide()) {
            client = CommonConstants.CONSUMER;
        }
        String baselog = "Client[" + client + "],InterfaceName=[" + invocation.getInvoker().getInterface().getSimpleName() + "],MethodName=[" + invocation.getMethodName() + "]";
        if (properties.getLogLevel() == RequestLogEnum.INFO) {
            log.info("DUBBO - 服务调用: {}", baselog);
        } else {
            log.info("DUBBO - 服务调用: {},Parameter={}", baselog, invocation.getArguments());
        }

        long startTime = System.currentTimeMillis();
        // 执行接口调用逻辑
        Result result = invoker.invoke(invocation);
        // 调用耗时
        long elapsed = System.currentTimeMillis() - startTime;
        // 如果发生异常 则打印异常日志
        if (result.hasException() && invoker.getInterface().equals(GenericService.class)) {
            log.error("DUBBO - 服务异常: {},Exception={}", baselog, result.getException());
        } else {
            if (properties.getLogLevel() == RequestLogEnum.INFO) {
                log.info("DUBBO - 服务响应: {},SpendTime=[{}ms]", baselog, elapsed);
            } else if (properties.getLogLevel() == RequestLogEnum.FULL) {
                log.info("DUBBO - 服务响应: {},SpendTime=[{}ms],Response={}", baselog, elapsed, JsonUtils.toJsonString(new Object[]{result.getValue()}));
            }
        }
        return result;
    }

}
