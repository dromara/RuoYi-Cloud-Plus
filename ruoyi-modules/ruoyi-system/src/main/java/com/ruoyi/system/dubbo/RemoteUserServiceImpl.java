package com.ruoyi.system.dubbo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.core.constant.UserConstants;
import com.ruoyi.common.core.enums.UserStatus;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.exception.user.UserException;
import com.ruoyi.system.api.RemoteUserService;
import com.ruoyi.system.api.domain.SysUser;
import com.ruoyi.system.api.model.LoginUser;
import com.ruoyi.system.api.model.RoleDTO;
import com.ruoyi.system.api.model.XcxLoginUser;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysPermissionService;
import com.ruoyi.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志记录
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
@DubboService
public class RemoteUserServiceImpl implements RemoteUserService {

    private final ISysUserService userService;
    private final ISysPermissionService permissionService;
    private final ISysConfigService configService;

    @Override
    public LoginUser getUserInfo(String username) throws UserException {
        SysUser sysUser = userService.selectUserByUserName(username);
        if (ObjectUtil.isNull(sysUser)) {
            throw new UserException("user.not.exists", username);
        }
        if (UserStatus.DELETED.getCode().equals(sysUser.getDelFlag())) {
            throw new UserException("user.password.delete", username);
        }
        if (UserStatus.DISABLE.getCode().equals(sysUser.getStatus())) {
            throw new UserException("user.blocked", username);
        }
        // 此处可根据登录用户的数据不同 自行创建 loginUser
        return buildLoginUser(sysUser);
    }

    @Override
    public LoginUser getUserInfoByPhonenumber(String phonenumber) throws UserException {
        SysUser sysUser = userService.selectUserByPhonenumber(phonenumber);
        if (ObjectUtil.isNull(sysUser)) {
            throw new UserException("user.not.exists", phonenumber);
        }
        if (UserStatus.DELETED.getCode().equals(sysUser.getDelFlag())) {
            throw new UserException("user.password.delete", phonenumber);
        }
        if (UserStatus.DISABLE.getCode().equals(sysUser.getStatus())) {
            throw new UserException("user.blocked", phonenumber);
        }
        // 此处可根据登录用户的数据不同 自行创建 loginUser
        return buildLoginUser(sysUser);
    }

    @Override
    public XcxLoginUser getUserInfoByOpenid(String openid) throws UserException {
        // todo 自行实现 userService.selectUserByOpenid(openid);
        SysUser sysUser = new SysUser();
        if (ObjectUtil.isNull(sysUser)) {
            // todo 用户不存在 业务逻辑自行实现
        }
        if (UserStatus.DELETED.getCode().equals(sysUser.getDelFlag())) {
            // todo 用户已被删除 业务逻辑自行实现
        }
        if (UserStatus.DISABLE.getCode().equals(sysUser.getStatus())) {
            // todo 用户已被停用 业务逻辑自行实现
        }
        // 此处可根据登录用户的数据不同 自行创建 loginUser
        XcxLoginUser loginUser = new XcxLoginUser();
        loginUser.setUserId(sysUser.getUserId());
        loginUser.setUsername(sysUser.getUserName());
        loginUser.setUserType(sysUser.getUserType());
        loginUser.setOpenid(openid);
        return loginUser;
    }

    @Override
    public Boolean registerUserInfo(SysUser sysUser) {
        String username = sysUser.getUserName();
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser")))) {
            throw new ServiceException("当前系统没有开启注册功能");
        }
        if (UserConstants.NOT_UNIQUE.equals(userService.checkUserNameUnique(username))) {
            throw new UserException("user.register.save.error", username);
        }
        return userService.registerUser(sysUser);
    }

    /**
     * 构建登录用户
     */
    private LoginUser buildLoginUser(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setUsername(user.getUserName());
        loginUser.setPassword(user.getPassword());
        loginUser.setUserType(user.getUserType());
        loginUser.setMenuPermission(permissionService.getMenuPermission(user.getUserId()));
        loginUser.setRolePermission(permissionService.getRolePermission(user.getUserId()));
        loginUser.setDeptName(user.getDept().getDeptName());
        List<RoleDTO> roles = BeanUtil.copyToList(user.getRoles(), RoleDTO.class);
        loginUser.setRoles(roles);
        return loginUser;
    }

}
