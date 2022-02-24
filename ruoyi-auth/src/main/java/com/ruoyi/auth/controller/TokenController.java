package com.ruoyi.auth.controller;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.ruoyi.auth.form.LoginBody;
import com.ruoyi.auth.form.RegisterBody;
import com.ruoyi.auth.service.SysLoginService;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.satoken.utils.LoginHelper;
import com.ruoyi.system.api.model.LoginUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * token 控制
 *
 * @author Lion Li
 */
@Validated
@Api(value = "认证鉴权控制器", tags = {"认证鉴权管理"})
@RequiredArgsConstructor
@RestController
public class TokenController {

    private final SysLoginService sysLoginService;

    @ApiOperation("登录方法")
    @PostMapping("login")
    public R<Map<String, Object>> login(@Validated @RequestBody LoginBody form) {
        // 用户登录
        String accessToken = sysLoginService.login(form.getUsername(), form.getPassword());

        // 接口返回信息
        Map<String, Object> rspMap = new HashMap<String, Object>();
        rspMap.put("access_token", accessToken);
        return R.ok(rspMap);
    }

    @ApiOperation("登出方法")
    @DeleteMapping("logout")
    public R<Void> logout() {
        try {
            StpUtil.logout();
            sysLoginService.logout(LoginHelper.getUsername());
        } catch (NotLoginException e) {
        }
        return R.ok();
    }

    @ApiOperation("用户注册")
    @PostMapping("register")
    public R<Void> register(@RequestBody RegisterBody registerBody) {
        // 用户注册
        sysLoginService.register(registerBody);
        return R.ok();
    }

}
