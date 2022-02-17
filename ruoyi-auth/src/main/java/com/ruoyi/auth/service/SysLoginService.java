package com.ruoyi.auth.service;

import cn.dev33.satoken.secure.BCrypt;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.auth.form.RegisterBody;
import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.constant.Constants;
import com.ruoyi.common.core.constant.UserConstants;
import com.ruoyi.common.core.enums.UserType;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.redis.utils.RedisUtils;
import com.ruoyi.system.api.RemoteLogService;
import com.ruoyi.system.api.RemoteUserService;
import com.ruoyi.system.api.domain.SysLogininfor;
import com.ruoyi.system.api.domain.SysUser;
import com.ruoyi.system.api.model.LoginUser;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录校验方法
 *
 * @author ruoyi
 */
@Service
public class SysLoginService {

    @DubboReference
    private RemoteLogService remoteLogService;
    @DubboReference
    private RemoteUserService remoteUserService;

    /**
     * 登录
     */
    public LoginUser login(String username, String password) {
        // 用户名或密码为空 错误
        if (StringUtils.isAnyBlank(username, password)) {
            recordLogininfor(username, Constants.LOGIN_FAIL, "用户/密码必须填写");
            throw new ServiceException("用户/密码必须填写");
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
            || password.length() > UserConstants.PASSWORD_MAX_LENGTH) {
            recordLogininfor(username, Constants.LOGIN_FAIL, "用户密码不在指定范围");
            throw new ServiceException("用户密码不在指定范围");
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
            || username.length() > UserConstants.USERNAME_MAX_LENGTH) {
            recordLogininfor(username, Constants.LOGIN_FAIL, "用户名不在指定范围");
            throw new ServiceException("用户名不在指定范围");
        }
        LoginUser userInfo;
        try {
            // 查询用户信息
            userInfo = remoteUserService.getUserInfo(username);

            if (ObjectUtil.isNull(userInfo)) {
                recordLogininfor(username, Constants.LOGIN_FAIL, "登录用户不存在");
                throw new ServiceException("登录用户：" + username + " 不存在");
            }
        } catch (Exception e) {
            recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage());
            throw new ServiceException(e.getMessage());
        }

        // 获取用户登录错误次数(可自定义限制策略 例如: key + username + ip)
        Integer errorNumber = RedisUtils.getCacheObject(CacheConstants.LOGIN_ERROR + username);
        // 锁定时间内登录 则踢出
        if (ObjectUtil.isNotNull(errorNumber) && errorNumber.equals(CacheConstants.LOGIN_ERROR_NUMBER)) {
            String msg = "密码错误次数过多，帐户锁定" + CacheConstants.LOGIN_ERROR_LIMIT_TIME + "分钟";
            recordLogininfor(username, Constants.LOGIN_FAIL, msg);
            throw new ServiceException(msg, null);
        }

        if (!BCrypt.checkpw(password, userInfo.getPassword())) {
            // 是否第一次
            errorNumber = ObjectUtil.isNull(errorNumber) ? 1 : errorNumber + 1;
            // 达到规定错误次数 则锁定登录
            if (errorNumber.equals(CacheConstants.LOGIN_ERROR_NUMBER)) {
                String msg = "密码错误次数过多，帐户锁定" + CacheConstants.LOGIN_ERROR_LIMIT_TIME + "分钟";
                RedisUtils.setCacheObject(CacheConstants.LOGIN_ERROR + username, errorNumber, CacheConstants.LOGIN_ERROR_LIMIT_TIME, TimeUnit.MINUTES);
                recordLogininfor(username, Constants.LOGIN_FAIL, msg);
                throw new ServiceException(msg, null);
            } else {
                // 未达到规定错误次数 则递增
                String msg = "密码输入错误" + errorNumber + "次";
                RedisUtils.setCacheObject(CacheConstants.LOGIN_ERROR + username, errorNumber);
                recordLogininfor(username, Constants.LOGIN_FAIL, msg);
                throw new ServiceException(msg, null);
            }
        }
        // 登录成功 清空错误次数
        RedisUtils.deleteObject(CacheConstants.LOGIN_ERROR + username);
        recordLogininfor(username, Constants.LOGIN_SUCCESS, "登录成功");
        return userInfo;
    }

    public void logout(String loginName) {
        recordLogininfor(loginName, Constants.LOGOUT, "退出成功");
    }

    /**
     * 注册
     */
    public void register(RegisterBody registerBody) {
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        // 校验用户类型是否存在
        String userType = UserType.getUserType(registerBody.getUserType()).getUserType();

        if (UserConstants.NOT_UNIQUE.equals(remoteUserService.checkUserNameUnique(username))) {
            throw new ServiceException("保存用户 " + username + " 失败，注册账号已存在");
        }
        // 注册用户信息
        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);
        sysUser.setNickName(username);
        sysUser.setPassword(BCrypt.hashpw(password));
        sysUser.setUserType(userType);
        boolean regFlag = remoteUserService.registerUserInfo(sysUser);
        if (!regFlag) {
            throw new ServiceException("注册失败，请联系系统管理人员");
        }
        recordLogininfor(username, Constants.REGISTER, "注册成功");
    }

    /**
     * 记录登录信息
     *
     * @param username 用户名
     * @param status   状态
     * @param message  消息内容
     * @return
     */
    public void recordLogininfor(String username, String status, String message) {
        SysLogininfor logininfor = new SysLogininfor();
        logininfor.setUserName(username);
        logininfor.setIpaddr(ServletUtils.getClientIP());
        logininfor.setMsg(message);
        // 日志状态
        if (StringUtils.equalsAny(status, Constants.LOGIN_SUCCESS, Constants.LOGOUT, Constants.REGISTER)) {
            logininfor.setStatus("0");
        } else if (Constants.LOGIN_FAIL.equals(status)) {
            logininfor.setStatus("1");
        }
        remoteLogService.saveLogininfor(logininfor);
    }
}
