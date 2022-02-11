package com.ruoyi.system.controller;

import cn.dev33.satoken.secure.BCrypt;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.core.constant.UserConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.satoken.utils.LoginHelper;
import com.ruoyi.resource.api.RemoteFileService;
import com.ruoyi.resource.api.domain.SysFile;
import com.ruoyi.system.api.domain.SysUser;
import com.ruoyi.system.service.ISysUserService;
import io.seata.spring.annotation.GlobalTransactional;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 个人信息 业务处理
 *
 * @author Lion Li
 */
@Validated
@Api(value = "个人信息控制器", tags = {"个人信息管理"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/user/profile")
public class SysProfileController extends BaseController {

    private final ISysUserService userService;

    @DubboReference
    private RemoteFileService remoteFileService;

    /**
     * 个人信息
     */
    @ApiOperation("个人信息")
    @GetMapping
    public R<Map<String, Object>> profile() {
        String username = LoginHelper.getUsername();
        SysUser user = userService.selectUserByUserName(username);
        Map<String, Object> ajax = new HashMap<>();
        ajax.put("user", user);
        ajax.put("roleGroup", userService.selectUserRoleGroup(username));
        ajax.put("postGroup", userService.selectUserPostGroup(username));
        return R.ok(ajax);
    }

    /**
     * 修改用户
     */
    @ApiOperation("修改用户")
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> updateProfile(@RequestBody SysUser user) {
        if (StringUtils.isNotEmpty(user.getPhonenumber())
                && UserConstants.NOT_UNIQUE.equals(userService.checkPhoneUnique(user))) {
            return R.fail("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        } else if (StringUtils.isNotEmpty(user.getEmail())
                && UserConstants.NOT_UNIQUE.equals(userService.checkEmailUnique(user))) {
            return R.fail("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setUserId(LoginHelper.getUserId());
        user.setUserName(null);
        user.setPassword(null);
        if (userService.updateUserProfile(user) > 0) {
            return R.ok();
        }
        return R.fail("修改个人信息异常，请联系管理员");
    }

    /**
     * 重置密码
     */
    @ApiOperation("重置密码")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "oldPassword", value = "旧密码", paramType = "query", dataTypeClass = String.class),
        @ApiImplicitParam(name = "newPassword", value = "新密码", paramType = "query", dataTypeClass = String.class)
    })
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping("/updatePwd")
    public R<Void> updatePwd(String oldPassword, String newPassword) {
        SysUser user = userService.selectUserById(LoginHelper.getUserId());
        String password = user.getPassword();
        if (!BCrypt.checkpw(oldPassword, password)) {
            return R.fail("修改密码失败，旧密码错误");
        }
        if (BCrypt.checkpw(newPassword, password)) {
            return R.fail("新密码不能与旧密码相同");
        }
        if (userService.resetUserPwd(user.getUserName(), BCrypt.hashpw(newPassword)) > 0) {
            return R.ok();
        }
        return R.fail("修改密码异常，请联系管理员");
    }

    /**
     * 头像上传
     */
    @GlobalTransactional(rollbackFor = Exception.class)
    @ApiOperation("头像上传")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "avatarfile", value = "用户头像", paramType = "query", dataTypeClass = File.class, required = true)
    })
    @Log(title = "用户头像", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    public R<Map<String, Object>> avatar(@RequestPart("avatarfile") MultipartFile file) throws IOException {
        if (!file.isEmpty()) {
            SysFile sysFile = remoteFileService.upload(file.getName(), file.getOriginalFilename(), file.getContentType(), file.getBytes());
            if (ObjectUtil.isNull(sysFile)) {
                return R.fail("文件服务异常，请联系管理员");
            }
            String url = sysFile.getUrl();
            if (userService.updateUserAvatar(LoginHelper.getUsername(), url)) {
                Map<String, Object> ajax = new HashMap<>();
                ajax.put("imgUrl", url);
                return R.ok(ajax);
            }
        }
        return R.fail("上传图片异常，请联系管理员");
    }
}
