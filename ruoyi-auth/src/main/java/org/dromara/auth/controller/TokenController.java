package org.dromara.auth.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.auth.domain.vo.LoginTenantVo;
import org.dromara.auth.domain.vo.LoginVo;
import org.dromara.auth.domain.vo.TenantListVo;
import org.dromara.common.core.domain.model.LoginBody;
import org.dromara.auth.form.RegisterBody;
import org.dromara.auth.service.IAuthStrategy;
import org.dromara.auth.service.SysLoginService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.social.config.properties.SocialLoginConfigProperties;
import org.dromara.common.social.config.properties.SocialProperties;
import org.dromara.common.social.utils.SocialUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.api.RemoteClientService;
import org.dromara.system.api.RemoteSocialService;
import org.dromara.system.api.RemoteTenantService;
import org.dromara.system.api.domain.vo.RemoteClientVo;
import org.dromara.system.api.domain.vo.RemoteTenantVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URL;
import java.util.List;

/**
 * token 控制
 *
 * @author Lion Li
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
public class TokenController {

    private final SocialProperties socialProperties;
    private final SysLoginService sysLoginService;

    @DubboReference
    private final RemoteTenantService remoteTenantService;
    @DubboReference
    private final RemoteClientService remoteClientService;
    @DubboReference
    private final RemoteSocialService remoteSocialService;

    /**
     * 登录方法
     */
    @PostMapping("login")
    public R<LoginVo> login(@Validated @RequestBody LoginBody loginBody) {
        // 授权类型和客户端id
        String clientId = loginBody.getClientId();
        String grantType = loginBody.getGrantType();
        RemoteClientVo clientVo = remoteClientService.queryByClientId(clientId);

        // 查询不到 client 或 client 内不包含 grantType
        if (ObjectUtil.isNull(clientVo) || !StringUtils.contains(clientVo.getGrantType(), grantType)) {
            log.info("客户端id: {} 认证类型：{} 异常!.", clientId, grantType);
            return R.fail(MessageUtils.message("auth.grant.type.error"));
        }
        // 校验租户
        sysLoginService.checkTenant(loginBody.getTenantId());
        // 登录
        return R.ok(IAuthStrategy.login(loginBody, clientVo));
    }

    /**
     * 第三方登录请求
     *
     * @param source 登录来源
     * @return 结果
     */
    @GetMapping("/binding/{source}")
    public R<String> authBinding(@PathVariable("source") String source) {
        SocialLoginConfigProperties obj = socialProperties.getType().get(source);
        if (ObjectUtil.isNull(obj)) {
            return R.fail(source + "平台账号暂不支持");
        }
        AuthRequest authRequest = SocialUtils.getAuthRequest(source, socialProperties);
        String authorizeUrl = authRequest.authorize(AuthStateUtils.createState());
        return R.ok("操作成功", authorizeUrl);
    }

    /**
     * 第三方登录回调业务处理 绑定授权
     *
     * @param loginBody 请求体
     * @return 结果
     */
    @PostMapping("/social/callback")
    public R<Void> socialCallback(@RequestBody LoginBody loginBody) {
        // 获取第三方登录信息
        AuthResponse<AuthUser> response = SocialUtils.loginAuth(loginBody, socialProperties);
        AuthUser authUserData = response.getData();
        // 判断授权响应是否成功
        if (!response.ok()) {
            return R.fail(response.getMsg());
        }
        sysLoginService.socialRegister(authUserData);
        return R.ok();
    }


    /**
     * 取消授权
     *
     * @param socialId socialId
     */
    @DeleteMapping(value = "/unlock/{socialId}")
    public R<Void> unlockSocial(@PathVariable Long socialId) {
        Boolean rows = remoteSocialService.deleteWithValidById(socialId);
        return rows ? R.ok() : R.fail("取消授权失败");
    }

    /**
     * 登出方法
     */
    @PostMapping("logout")
    public R<Void> logout() {
        sysLoginService.logout();
        return R.ok();
    }

    /**
     * 用户注册
     */
    @PostMapping("register")
    public R<Void> register(@RequestBody RegisterBody registerBody) {
        // 用户注册
        sysLoginService.register(registerBody);
        return R.ok();
    }

    /**
     * 登录页面租户下拉框
     *
     * @return 租户列表
     */
    @GetMapping("/tenant/list")
    public R<LoginTenantVo> tenantList(HttpServletRequest request) throws Exception {
        List<RemoteTenantVo> tenantList = remoteTenantService.queryList();
        List<TenantListVo> voList = MapstructUtils.convert(tenantList, TenantListVo.class);
        // 获取域名
        String host;
        String referer = request.getHeader("referer");
        if (StringUtils.isNotBlank(referer)) {
            // 这里从referer中取值是为了本地使用hosts添加虚拟域名，方便本地环境调试
            host = referer.split("//")[1].split("/")[0];
        } else {
            host = new URL(request.getRequestURL().toString()).getHost();
        }
        // 根据域名进行筛选
        List<TenantListVo> list = StreamUtils.filter(voList, vo ->
            StringUtils.equals(vo.getDomain(), host));
        // 返回对象
        LoginTenantVo vo = new LoginTenantVo();
        vo.setVoList(CollUtil.isNotEmpty(list) ? list : voList);
        vo.setTenantEnabled(TenantHelper.isEnable());
        return R.ok(vo);
    }

}
