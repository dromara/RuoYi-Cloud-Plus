package org.dromara.auth.service.impl;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.auth.domain.vo.LoginVo;
import org.dromara.auth.service.IAuthStrategy;
import org.dromara.auth.service.SysLoginService;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.domain.model.LoginBody;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.core.utils.ValidatorUtils;
import org.dromara.common.core.validate.auth.SocialGroup;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.social.config.properties.SocialProperties;
import org.dromara.common.social.utils.SocialUtils;
import org.dromara.system.api.RemoteSocialService;
import org.dromara.system.api.RemoteUserService;
import org.dromara.system.api.domain.vo.RemoteClientVo;
import org.dromara.system.api.domain.vo.RemoteSocialVo;
import org.dromara.system.api.model.LoginUser;
import org.springframework.stereotype.Service;

/**
 * 第三方授权策略
 *
 * @author thiszhc is 三三
 */
@Slf4j
@Service("social" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class SocialAuthStrategy implements IAuthStrategy {

    private final SocialProperties socialProperties;
    private final SysLoginService loginService;

    @DubboReference
    private RemoteSocialService remoteSocialService;
    @DubboReference
    private RemoteUserService remoteUserService;

    @Override
    public void validate(LoginBody loginBody) {
        ValidatorUtils.validate(loginBody, SocialGroup.class);
    }

    /**
     * 登录-第三方授权登录
     *
     * @param clientId  客户端id
     * @param loginBody 登录信息
     * @param client    客户端信息
     */
    @Override
    public LoginVo login(String clientId, LoginBody loginBody, RemoteClientVo client) {
        AuthResponse<AuthUser> response = SocialUtils.loginAuth(loginBody, socialProperties);
        if (!response.ok()) {
            throw new ServiceException(response.getMsg());
        }
        AuthUser authUserData = response.getData();
        if ("GITEE".equals(authUserData.getSource())) {
            // 如用户使用 gitee 登录顺手 star 给作者一点支持 拒绝白嫖
            HttpUtil.createRequest(Method.PUT, "https://gitee.com/api/v5/user/starred/dromara/RuoYi-Vue-Plus")
                .formStr(MapUtil.of("access_token", authUserData.getToken().getAccessToken()))
                .executeAsync();
            HttpUtil.createRequest(Method.PUT, "https://gitee.com/api/v5/user/starred/dromara/RuoYi-Cloud-Plus")
                .formStr(MapUtil.of("access_token", authUserData.getToken().getAccessToken()))
                .executeAsync();
        }

        RemoteSocialVo socialVo = remoteSocialService.selectByAuthId(authUserData.getSource() + authUserData.getUuid());
        if (!ObjectUtil.isNotNull(socialVo)) {
            throw new ServiceException("你还没有绑定第三方账号，绑定后才可以登录！");
        }
        // 验证授权表里面的租户id是否包含当前租户id
        String tenantId = socialVo.getTenantId();
        if (ObjectUtil.isNotNull(socialVo) && StrUtil.isNotBlank(tenantId)
            && !tenantId.contains(loginBody.getTenantId())) {
            throw new ServiceException("对不起，你没有权限登录当前租户！");
        }

        LoginUser loginUser = remoteUserService.getUserInfo(socialVo.getUserName(), tenantId);
        SaLoginModel model = new SaLoginModel();
        model.setDevice(client.getDeviceType());
        // 自定义分配 不同用户体系 不同 token 授权时间 不设置默认走全局 yml 配置
        // 例如: 后台用户30分钟过期 app用户1天过期
        model.setTimeout(client.getTimeout());
        model.setActiveTimeout(client.getActiveTimeout());
        // 生成token
        LoginHelper.login(loginUser, model);

        loginService.recordLogininfor(loginUser.getTenantId(), socialVo.getUserName(), Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        remoteUserService.recordLoginInfo(socialVo.getUserId());

        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(StpUtil.getTokenValue());
        loginVo.setExpireIn(StpUtil.getTokenTimeout());
        loginVo.setClientId(clientId);
        return loginVo;
    }

}
