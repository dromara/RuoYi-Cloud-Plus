package org.dromara.gateway.filter;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.httpauth.basic.SaHttpBasicUtil;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import cn.dev33.satoken.util.SaTokenConsts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.utils.NetUtils;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.gateway.config.properties.IgnoreWhiteProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * [Sa-Token 权限认证] 过滤器配置
 *
 * @author Lion Li
 */
@Configuration
public class AuthFilter {

    private static final String CLIENT_RULE_SEPARATOR_REGEX = "[,;\\r\\n]+";

    private final IgnoreWhiteProperties ignoreWhite;

    public AuthFilter(IgnoreWhiteProperties ignoreWhite) {
        this.ignoreWhite = ignoreWhite;
    }

    /**
     * 注册 Sa-Token 全局过滤器
     */
    @Bean
    public SaServletFilter authSaServletFilter() {
        return new SaServletFilter()
            .addInclude("/**")
            .addExclude("/favicon.ico", "/actuator", "/actuator/**", "/error")
            .setAuth(obj -> SaRouter.match("/**")
                .notMatch(ignoreWhite.getWhites())
                .check(() -> {
                    HttpServletRequest request = ServletUtils.getRequest();

                    StpUtil.checkLogin();

                    String headerCid = request.getHeader(LoginHelper.CLIENT_KEY);
                    String paramCid = ServletUtils.getParameter(LoginHelper.CLIENT_KEY);
                    Object extra = StpUtil.getExtra(LoginHelper.CLIENT_KEY);
                    String clientId = extra == null ? null : extra.toString();
                    if (!StringUtils.equalsAny(clientId, headerCid, paramCid)) {
                        throw NotLoginException.newInstance(StpUtil.getLoginType(),
                            "-100", "客户端ID与Token不匹配",
                            StpUtil.getTokenValue());
                    }
                    validateClientAccessRules(request);
                }))
            .setError(e -> {
                HttpServletResponse response = ServletUtils.getResponse();
                response.setContentType(SaTokenConsts.CONTENT_TYPE_APPLICATION_JSON);
                if (e instanceof NotLoginException) {
                    return SaResult.error(e.getMessage()).setCode(HttpStatus.UNAUTHORIZED);
                }
                return SaResult.error("认证失败，无法访问系统资源").setCode(HttpStatus.UNAUTHORIZED);
            });
    }

    /**
     * 为 actuator 健康检查接口配置 Basic Auth 鉴权过滤器。
     *
     * @return Sa-Token Servlet 过滤器
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        String username = SpringUtils.getProperty("spring.cloud.nacos.discovery.metadata.username");
        String password = SpringUtils.getProperty("spring.cloud.nacos.discovery.metadata.userpassword");
        return new SaServletFilter()
            .addInclude("/actuator", "/actuator/**")
            .setAuth(obj -> {
                SaHttpBasicUtil.check(username + StringUtils.COLON + password);
            })
            .setError(e -> {
                HttpServletResponse response = ServletUtils.getResponse();
                response.setContentType(SaTokenConsts.CONTENT_TYPE_APPLICATION_JSON);
                return SaResult.error(e.getMessage()).setCode(HttpStatus.UNAUTHORIZED);
            });
    }

    /**
     * 按客户端配置校验接口访问路径与来源IP。
     */
    private void validateClientAccessRules(HttpServletRequest request) {
        String requestPath = StringUtils.blankToDefault(request.getServletPath(), request.getRequestURI());
        String accessPath = getTokenExtra(LoginHelper.CLIENT_ACCESS_PATH_KEY);
        if (StringUtils.isNotBlank(accessPath)) {
            List<String> accessPathList = StringUtils.str2List(accessPath, CLIENT_RULE_SEPARATOR_REGEX, true, true);
            if (!StringUtils.matches(requestPath, accessPathList)) {
                throw new NotPermissionException("当前客户端未授权访问该接口路径");
            }
        }

        String ipWhitelist = getTokenExtra(LoginHelper.CLIENT_IP_WHITELIST_KEY);
        if (StringUtils.isNotBlank(ipWhitelist)) {
            String clientIp = ServletUtils.getClientIP(request);
            List<String> ipWhitelistList = StringUtils.str2List(ipWhitelist, CLIENT_RULE_SEPARATOR_REGEX, true, true);
            boolean matched = ipWhitelistList.stream().anyMatch(rule -> NetUtils.isMatchIpRule(rule, clientIp));
            if (!matched) {
                throw new NotPermissionException("当前客户端IP不在白名单内");
            }
        }
    }

    /**
     * 读取token扩展信息，兼容空值场景。
     */
    private String getTokenExtra(String key) {
        Object extra = StpUtil.getExtra(key);
        return extra == null ? null : extra.toString();
    }

}
