package com.ruoyi.system.api;

import com.ruoyi.system.api.domain.SysUser;
import com.ruoyi.system.api.model.LoginUser;

/**
 * 用户服务
 *
 * @author Lion Li
 */
public interface RemoteUserService {

    /**
     * 通过用户名查询用户信息
     *
     * @param username 用户名
     * @return 结果
     */
    LoginUser getUserInfo(String username);

    /**
     * 注册用户信息
     *
     * @param sysUser 用户信息
     * @return 结果
     */
    Boolean registerUserInfo(SysUser sysUser);
}
