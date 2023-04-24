package cn.dev33.satoken.context.dubbo.filter;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextDefaultImpl;
import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.spring.SaBeanInject;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaTokenConsts;
import org.dromara.common.core.utils.SpringUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

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

	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 强制初始化 Sa-Token 相关配置 解决内网鉴权元数据加载报错问题
        SpringUtils.getBean(SaBeanInject.class);

        // 追加 Id-Token 参数
		if(SaManager.getConfig().getCheckSameToken()) {
            RpcContext.getServiceContext().setAttachment(SaSameUtil.SAME_TOKEN, SaSameUtil.getToken());
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
