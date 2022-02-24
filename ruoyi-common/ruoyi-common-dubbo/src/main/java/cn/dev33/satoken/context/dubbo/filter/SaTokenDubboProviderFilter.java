package cn.dev33.satoken.context.dubbo.filter;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.id.SaIdUtil;
import cn.dev33.satoken.spring.SaBeanInject;
import com.ruoyi.common.core.utils.SpringUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

/**
 *
 * Sa-Token 整合 Dubbo Provider端过滤器
 *
 * 此过滤器为覆盖 Sa-Token 包内过滤器
 *
 * @author kong
 *
 */
@Activate(group = {CommonConstants.PROVIDER}, order = Integer.MIN_VALUE)
public class SaTokenDubboProviderFilter implements Filter {

	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 强制初始化 Sa-Token 相关配置 解决内网鉴权元数据加载报错问题
        SpringUtils.getBean(SaBeanInject.class);

        // RPC 调用鉴权
		if(SaManager.getConfig().getCheckIdToken()) {
			String idToken = invocation.getAttachment(SaIdUtil.ID_TOKEN);
            SaIdUtil.checkToken(idToken);
		}

		// 开始调用
		return invoker.invoke(invocation);
	}

}
