package com.ruoyi.auth.service;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.auth.form.RegisterBody;
import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.constant.Constants;
import com.ruoyi.common.core.enums.DeviceType;
import com.ruoyi.common.core.enums.UserType;
import com.ruoyi.common.core.exception.user.UserException;
import com.ruoyi.common.core.utils.MessageUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.redis.utils.RedisUtils;
import com.ruoyi.common.satoken.utils.LoginHelper;
import com.ruoyi.system.api.RemoteLogService;
import com.ruoyi.system.api.RemoteUserService;
import com.ruoyi.system.api.domain.SysLogininfor;
import com.ruoyi.system.api.domain.SysUser;
import com.ruoyi.system.api.model.LoginUser;
import com.ruoyi.system.api.model.XcxLoginUser;
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
    public String login(String username, String password) {
        LoginUser userInfo = remoteUserService.getUserInfo(username);

        // 获取用户登录错误次数(可自定义限制策略 例如: key + username + ip)
        Integer errorNumber = RedisUtils.getCacheObject(CacheConstants.LOGIN_ERROR + username);
        // 锁定时间内登录 则踢出
        if (ObjectUtil.isNotNull(errorNumber) && errorNumber.equals(CacheConstants.LOGIN_ERROR_NUMBER)) {
            recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.retry.limit.exceed", CacheConstants.LOGIN_ERROR_LIMIT_TIME));
            throw new UserException("user.password.retry.limit.exceed", CacheConstants.LOGIN_ERROR_LIMIT_TIME);
        }

        if (!BCrypt.checkpw(password, userInfo.getPassword())) {
            // 是否第一次
            errorNumber = ObjectUtil.isNull(errorNumber) ? 1 : errorNumber + 1;
            // 达到规定错误次数 则锁定登录
            if (errorNumber.equals(CacheConstants.LOGIN_ERROR_NUMBER)) {
                RedisUtils.setCacheObject(CacheConstants.LOGIN_ERROR + username, errorNumber, CacheConstants.LOGIN_ERROR_LIMIT_TIME, TimeUnit.MINUTES);
                recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.retry.limit.exceed", CacheConstants.LOGIN_ERROR_LIMIT_TIME));
                throw new UserException("user.password.retry.limit.exceed", CacheConstants.LOGIN_ERROR_LIMIT_TIME);
            } else {
                // 未达到规定错误次数 则递增
                RedisUtils.setCacheObject(CacheConstants.LOGIN_ERROR + username, errorNumber);
                recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.retry.limit.count", errorNumber));
                throw new UserException("user.password.retry.limit.count", errorNumber);
            }
        }
        // 登录成功 清空错误次数
        RedisUtils.deleteObject(CacheConstants.LOGIN_ERROR + username);
        // 获取登录token
        LoginHelper.loginByDevice(userInfo, DeviceType.PC);

        recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        return StpUtil.getTokenValue();
    }

    public String smsLogin(String phonenumber, String smsCode) {
        // 通过手机号查找用户
        LoginUser userInfo = remoteUserService.getUserInfoByPhonenumber(phonenumber);

        // 获取用户登录错误次数(可自定义限制策略 例如: key + username + ip)
        Integer errorNumber = RedisUtils.getCacheObject(CacheConstants.LOGIN_ERROR + userInfo.getUsername());
        // 锁定时间内登录 则踢出
        if (ObjectUtil.isNotNull(errorNumber) && errorNumber.equals(CacheConstants.LOGIN_ERROR_NUMBER)) {
            recordLogininfor(userInfo.getUsername(), Constants.LOGIN_FAIL, MessageUtils.message("sms.code.retry.limit.exceed", CacheConstants.LOGIN_ERROR_LIMIT_TIME));
            throw new UserException("sms.code.retry.limit.exceed", CacheConstants.LOGIN_ERROR_LIMIT_TIME);
        }

        if (!validateSmsCode(phonenumber, smsCode)) {
            // 是否第一次
            errorNumber = ObjectUtil.isNull(errorNumber) ? 1 : errorNumber + 1;
            // 达到规定错误次数 则锁定登录
            if (errorNumber.equals(CacheConstants.LOGIN_ERROR_NUMBER)) {
                RedisUtils.setCacheObject(CacheConstants.LOGIN_ERROR + userInfo.getUsername(), errorNumber, CacheConstants.LOGIN_ERROR_LIMIT_TIME, TimeUnit.MINUTES);
                recordLogininfor(userInfo.getUsername(), Constants.LOGIN_FAIL, MessageUtils.message("sms.code.retry.limit.exceed", CacheConstants.LOGIN_ERROR_LIMIT_TIME));
                throw new UserException("sms.code.retry.limit.exceed", CacheConstants.LOGIN_ERROR_LIMIT_TIME);
            } else {
                // 未达到规定错误次数 则递增
                RedisUtils.setCacheObject(CacheConstants.LOGIN_ERROR + userInfo.getUsername(), errorNumber);
                recordLogininfor(userInfo.getUsername(), Constants.LOGIN_FAIL, MessageUtils.message("sms.code.retry.limit.count", errorNumber));
                throw new UserException("sms.code.retry.limit.count", errorNumber);
            }
        }

        // 登录成功 清空错误次数
        RedisUtils.deleteObject(CacheConstants.LOGIN_ERROR + userInfo.getUsername());

        // 生成token
        LoginHelper.loginByDevice(userInfo, DeviceType.APP);

        recordLogininfor(userInfo.getUsername(), Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        return StpUtil.getTokenValue();
    }

    public String xcxLogin(String xcxCode) {
        // xcxCode 为 小程序调用 wx.login 授权后获取
        // todo 自行实现 校验 appid + appsrcret + xcxCode 调用登录凭证校验接口 获取 session_key 与 openid
        String openid = "";
        XcxLoginUser userInfo = remoteUserService.getUserInfoByOpenid(openid);
        // 生成token
        LoginHelper.loginByDevice(userInfo, DeviceType.XCX);

        recordLogininfor(userInfo.getUsername(), Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        return StpUtil.getTokenValue();
    }

    public void logout(String loginName) {
        recordLogininfor(loginName, Constants.LOGOUT, MessageUtils.message("user.logout.success"));
    }

    /**
     * 注册
     */
    public void register(RegisterBody registerBody) {
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        // 校验用户类型是否存在
        String userType = UserType.getUserType(registerBody.getUserType()).getUserType();

        // 注册用户信息
        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);
        sysUser.setNickName(username);
        sysUser.setPassword(BCrypt.hashpw(password));
        sysUser.setUserType(userType);
        boolean regFlag = remoteUserService.registerUserInfo(sysUser);
        if (!regFlag) {
            throw new UserException("user.register.error");
        }
        recordLogininfor(username, Constants.REGISTER, MessageUtils.message("user.register.success"));
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
            logininfor.setStatus(Constants.LOGIN_SUCCESS_STATUS);
        } else if (Constants.LOGIN_FAIL.equals(status)) {
            logininfor.setStatus(Constants.LOGIN_FAIL_STATUS);
        }
        remoteLogService.saveLogininfor(logininfor);
    }

    /**
     * 校验短信验证码
     */
    private boolean validateSmsCode(String phonenumber, String smsCode) {
        // todo 此处使用手机号查询redis验证码与参数验证码是否一致 用户自行实现
        return true;
    }
}
