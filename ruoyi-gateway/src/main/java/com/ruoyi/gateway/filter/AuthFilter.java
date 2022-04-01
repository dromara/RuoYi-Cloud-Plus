package com.ruoyi.gateway.filter;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.gateway.config.properties.IgnoreWhiteProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [Sa-Token 权限认证] 拦截器
 * @author Lion Li
 */
@Configuration
public class AuthFilter {

    // 注册 Sa-Token全局过滤器
    @Bean
    public SaReactorFilter getSaReactorFilter(IgnoreWhiteProperties ignoreWhite) {
        return new SaReactorFilter()
            // 拦截地址
            .addInclude("/**")
            // 开放地址
            .setExcludeList(ignoreWhite.getWhites())
            .addExclude("/favicon.ico", "/actuator/**")
            // 鉴权方法：每次访问进入
            .setAuth(obj -> {
                // 登录校验 -- 拦截所有路由
                SaRouter.match("/**", r -> {
                    // 检查是否登录 是否有token
                    StpUtil.checkLogin();

                    // 有效率影响 用于临时测试
                    // if (log.isDebugEnabled()) {
                    //     log.debug("剩余有效时间: {}", StpUtil.getTokenTimeout());
                    //     log.debug("临时有效时间: {}", StpUtil.getTokenActivityTimeout());
                    // }
                });
            }).setError(e -> SaResult.error("认证失败，无法访问系统资源").setCode(HttpStatus.UNAUTHORIZED));
    }
}
