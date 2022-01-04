//package com.ruoyi.common.security.feign;
//
//import com.ruoyi.common.core.constant.SecurityConstants;
//import com.ruoyi.common.core.utils.ServletUtils;
//import com.ruoyi.common.core.utils.StringUtils;
//import com.ruoyi.common.core.utils.ip.IpUtils;
//import org.apache.dubbo.common.constants.CommonConstants;
//import org.apache.dubbo.common.extension.Activate;
//import org.apache.dubbo.rpc.*;
//import org.springframework.stereotype.Component;
//
//import javax.servlet.http.HttpServletRequest;
//import java.util.Map;
//
///**
// * feign 请求拦截器
// *
// * @author ruoyi
// */
//@Activate(group = {CommonConstants.CONSUMER}, order = -10000)
//@Component
//public class DubboRequestFilter implements Filter {
//    @Override
//    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
//        //执行接口调用逻辑
//        Result result = invoker.invoke(invocation);
//        HttpServletRequest httpServletRequest = ServletUtils.getRequest();
//        if (httpServletRequest != null) {
//            Map<String, String> headers = ServletUtils.getHeaders(httpServletRequest);
//            // 传递用户信息请求头，防止丢失
//            String userId = headers.get(SecurityConstants.DETAILS_USER_ID);
//            if (StringUtils.isNotEmpty(userId)) {
//                RpcContext.getServerContext().setAttachment(SecurityConstants.DETAILS_USER_ID, userId);
//            }
//            String userName = headers.get(SecurityConstants.DETAILS_USERNAME);
//            if (StringUtils.isNotEmpty(userName)) {
//                RpcContext.getServerContext().setAttachment(SecurityConstants.DETAILS_USERNAME, userName);
//            }
//            String authentication = headers.get(SecurityConstants.AUTHORIZATION_HEADER);
//            if (StringUtils.isNotEmpty(authentication)) {
//                RpcContext.getServerContext().setAttachment(SecurityConstants.AUTHORIZATION_HEADER, authentication);
//            }
//
//            // 配置客户端IP
//            RpcContext.getServerContext().setAttachment("X-Forwarded-For", IpUtils.getIpAddr(ServletUtils.getRequest()));
//        }
//        return result;
//    }
//}