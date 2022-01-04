package com.ruoyi.system.dubbo;

import com.ruoyi.common.core.constant.UserConstants;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.api.RemoteUserService;
import com.ruoyi.system.api.domain.SysUser;
import com.ruoyi.system.api.model.LoginUser;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysPermissionService;
import com.ruoyi.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 操作日志记录
 *
 * @author Lion Li
 */
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
@DubboService
public class RemoteUserServiceImpl implements RemoteUserService {

    private final ISysUserService userService;
    private final ISysPermissionService permissionService;
    private final ISysConfigService configService;

    @Override
    public LoginUser getUserInfo(String username, String source) {
        SysUser sysUser = userService.selectUserByUserName(username);
        if (StringUtils.isNull(sysUser)) {
            throw new ServiceException("用户名或密码错误");
        }
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(sysUser.getUserId());
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(sysUser.getUserId());
        LoginUser sysUserVo = new LoginUser();
        sysUserVo.setSysUser(sysUser);
        sysUserVo.setRoles(roles);
        sysUserVo.setPermissions(permissions);
        return sysUserVo;
    }

    @Override
    public Boolean registerUserInfo(SysUser sysUser, String source) {
        String username = sysUser.getUserName();
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser")))) {
            throw new ServiceException("当前系统没有开启注册功能");
        }
        if (UserConstants.NOT_UNIQUE.equals(userService.checkUserNameUnique(username))) {
            throw new ServiceException("保存用户'" + username + "'失败，注册账号已存在");
        }
        return userService.registerUser(sysUser);
    }
}
