package cn.dev33.satoken.context.dubbo.filter;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextDefaultImpl;
import cn.dev33.satoken.id.SaIdUtil;
import cn.dev33.satoken.spring.SaBeanInject;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaTokenConsts;
import cn.hutool.core.annotation.AnnotationUtil;
import com.ruoyi.common.core.annotation.InnerExclude;
import com.ruoyi.common.core.utils.SpringUtils;
import lombok.SneakyThrows;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import java.lang.reflect.Method;

/**
 *
 * Sa-Token 整合 Dubbo Consumer端过滤器
 *
 * 此过滤器为覆盖 Sa-Token 包内过滤器
 *
 * @author kong
 *
 */
@Activate(group = {CommonConstants.CONSUMER}, order = Integer.MIN_VALUE)
public class SaTokenDubboConsumerFilter implements Filter {

    @SneakyThrows(NoSuchMethodException.class)
	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 强制初始化 Sa-Token 相关配置 解决内网鉴权元数据加载报错问题
        SpringUtils.getBean(SaBeanInject.class);

        // 追加 Id-Token 参数
		if(SaManager.getConfig().getCheckIdToken()) {
            Class<?> clazz = invoker.getInterface();
            Method method = clazz.getMethod(invocation.getMethodName(), invocation.getParameterTypes());
            // 检查是否有内网鉴权排除注解
            if (AnnotationUtil.hasAnnotation(clazz, InnerExclude.class)
                || AnnotationUtil.hasAnnotation(method, InnerExclude.class)) {
                // 不传递 Id-Token
            } else {
                RpcContext.getServiceContext().setAttachment(SaIdUtil.ID_TOKEN, SaIdUtil.getToken());
            }

		}

		// 1. 调用前，向下传递会话Token
		if(SaManager.getSaTokenContextOrSecond() != SaTokenContextDefaultImpl.defaultContext) {
			RpcContext.getServiceContext().setAttachment(SaTokenConsts.JUST_CREATED, StpUtil.getTokenValueNotCut());
		}

		// 2. 开始调用
		Result invoke = invoker.invoke(invocation);

		// 3. 调用后，解析回传的Token值
		StpUtil.setTokenValue(invoke.getAttachment(SaTokenConsts.JUST_CREATED_NOT_PREFIX));

		// note
		return invoke;
	}

}
