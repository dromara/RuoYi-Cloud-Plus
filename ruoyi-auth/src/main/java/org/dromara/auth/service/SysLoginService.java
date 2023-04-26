package org.dromara.auth.service;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.auth.form.RegisterBody;
import org.dromara.auth.properties.UserPasswordProperties;
import org.dromara.common.core.constant.CacheConstants;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.enums.DeviceType;
import org.dromara.common.core.enums.LoginType;
import org.dromara.common.core.enums.TenantStatus;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.core.exception.user.CaptchaExpireException;
import org.dromara.common.core.exception.user.UserException;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.log.event.LogininforEvent;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.tenant.exception.TenantException;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.api.RemoteTenantService;
import org.dromara.system.api.RemoteUserService;
import org.dromara.system.api.domain.bo.RemoteUserBo;
import org.dromara.system.api.domain.vo.RemoteTenantVo;
import org.dromara.system.api.model.LoginUser;
import org.dromara.system.api.model.XcxLoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.function.Supplier;

/**
 * 登录校验方法
 *
 * @author ruoyi
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysLoginService {

    @DubboReference
    private RemoteUserService remoteUserService;
    @DubboReference
    private RemoteTenantService remoteTenantService;

    @Autowired
    private UserPasswordProperties userPasswordProperties;

    /**
     * 登录
     */
    public String login(String tenantId, String username, String password) {
        // 校验租户
        checkTenant(tenantId);
        LoginUser userInfo = remoteUserService.getUserInfo(username);
        checkLogin(LoginType.PASSWORD, tenantId, username, () -> !BCrypt.checkpw(password, userInfo.getPassword()));
        // 获取登录token
        LoginHelper.loginByDevice(userInfo, DeviceType.PC);

        recordLogininfor(tenantId, username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        return StpUtil.getTokenValue();
    }

    public String smsLogin(String tenantId, String phonenumber, String smsCode) {
        // 校验租户
        checkTenant(tenantId);
        // 通过手机号查找用户
        LoginUser userInfo = remoteUserService.getUserInfoByPhonenumber(phonenumber);

        checkLogin(LoginType.SMS, tenantId, userInfo.getUsername(), () -> !validateSmsCode(tenantId, phonenumber, smsCode));
        // 生成token
        LoginHelper.loginByDevice(userInfo, DeviceType.APP);

        recordLogininfor(tenantId, userInfo.getUsername(), Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        return StpUtil.getTokenValue();
    }

    public String emailLogin(String tenantId, String email, String emailCode) {
        // 校验租户
        checkTenant(tenantId);
        // 通过邮箱查找用户
        LoginUser userInfo = remoteUserService.getUserInfoByEmail(email);

        checkLogin(LoginType.EMAIL,tenantId, userInfo.getUsername(), () -> !validateEmailCode(tenantId, email, emailCode));
        // 生成token
        LoginHelper.loginByDevice(userInfo, DeviceType.APP);

        recordLogininfor(tenantId, userInfo.getUsername(), Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        return StpUtil.getTokenValue();
    }

    public String xcxLogin(String xcxCode) {
        // xcxCode 为 小程序调用 wx.login 授权后获取
        // todo 自行实现 校验 appid + appsrcret + xcxCode 调用登录凭证校验接口 获取 session_key 与 openid
        String openid = "";
        XcxLoginUser userInfo = remoteUserService.getUserInfoByOpenid(openid);
        // 校验租户
        checkTenant(userInfo.getTenantId());
        // 生成token
        LoginHelper.loginByDevice(userInfo, DeviceType.XCX);

        recordLogininfor(userInfo.getTenantId(), userInfo.getUsername(), Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        return StpUtil.getTokenValue();
    }

    /**
     * 退出登录
     */
    public void logout() {
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            if (TenantHelper.isEnable() && LoginHelper.isSuperAdmin()) {
                // 超级管理员 登出清除动态租户
                TenantHelper.clearDynamic();
            }
            StpUtil.logout();
            recordLogininfor(loginUser.getTenantId(), loginUser.getUsername(), Constants.LOGOUT, MessageUtils.message("user.logout.success"));
        } catch (NotLoginException ignored) {
        }
    }

    /**
     * 注册
     */
    public void register(RegisterBody registerBody) {
        String tenantId = registerBody.getTenantId();
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        // 校验用户类型是否存在
        String userType = UserType.getUserType(registerBody.getUserType()).getUserType();

        // 注册用户信息
        RemoteUserBo remoteUserBo = new RemoteUserBo();
        remoteUserBo.setUserName(username);
        remoteUserBo.setNickName(username);
        remoteUserBo.setPassword(BCrypt.hashpw(password));
        remoteUserBo.setUserType(userType);
        boolean regFlag = remoteUserService.registerUserInfo(remoteUserBo);
        if (!regFlag) {
            throw new UserException("user.register.error");
        }
        recordLogininfor(tenantId, username, Constants.REGISTER, MessageUtils.message("user.register.success"));
    }

    /**
     * 记录登录信息
     *
     * @param username 用户名
     * @param status   状态
     * @param message  消息内容
     * @return
     */
    public void recordLogininfor(String tenantId, String username, String status, String message) {
        // 封装对象
        LogininforEvent logininforEvent = new LogininforEvent();
        logininforEvent.setTenantId(tenantId);
        logininforEvent.setUsername(username);
        logininforEvent.setStatus(status);
        logininforEvent.setMessage(message);
        logininforEvent.setRequest(ServletUtils.getRequest());
        SpringUtils.context().publishEvent(logininforEvent);
    }

    /**
     * 校验短信验证码
     */
    private boolean validateSmsCode(String tenantId, String phonenumber, String smsCode) {
        String code = RedisUtils.getCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + phonenumber);
        if (StringUtils.isBlank(code)) {
            recordLogininfor(tenantId, phonenumber, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        return code.equals(smsCode);
    }

    /**
     * 校验邮箱验证码
     */
    private boolean validateEmailCode(String tenantId, String email, String emailCode) {
        String code = RedisUtils.getCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + email);
        if (StringUtils.isBlank(code)) {
            recordLogininfor(tenantId, email, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        return code.equals(emailCode);
    }

    /**
     * 登录校验
     */
    private void checkLogin(LoginType loginType, String tenantId, String username, Supplier<Boolean> supplier) {
        String errorKey = CacheConstants.PWD_ERR_CNT_KEY + username;
        String loginFail = Constants.LOGIN_FAIL;
        Integer maxRetryCount = userPasswordProperties.getMaxRetryCount();
        Integer lockTime = userPasswordProperties.getLockTime();

        // 获取用户登录错误次数(可自定义限制策略 例如: key + username + ip)
        Integer errorNumber = RedisUtils.getCacheObject(errorKey);
        // 锁定时间内登录 则踢出
        if (ObjectUtil.isNotNull(errorNumber) && errorNumber.equals(maxRetryCount)) {
            recordLogininfor(tenantId, username, loginFail, MessageUtils.message(loginType.getRetryLimitExceed(), maxRetryCount, lockTime));
            throw new UserException(loginType.getRetryLimitExceed(), maxRetryCount, lockTime);
        }

        if (supplier.get()) {
            // 是否第一次
            errorNumber = ObjectUtil.isNull(errorNumber) ? 1 : errorNumber + 1;
            // 达到规定错误次数 则锁定登录
            if (errorNumber.equals(maxRetryCount)) {
                RedisUtils.setCacheObject(errorKey, errorNumber, Duration.ofMinutes(lockTime));
                recordLogininfor(tenantId, username, loginFail, MessageUtils.message(loginType.getRetryLimitExceed(), maxRetryCount, lockTime));
                throw new UserException(loginType.getRetryLimitExceed(), maxRetryCount, lockTime);
            } else {
                // 未达到规定错误次数 则递增
                RedisUtils.setCacheObject(errorKey, errorNumber);
                recordLogininfor(tenantId, username, loginFail, MessageUtils.message(loginType.getRetryLimitCount(), errorNumber));
                throw new UserException(loginType.getRetryLimitCount(), errorNumber);
            }
        }
        // 登录成功 清空错误次数
        RedisUtils.deleteObject(errorKey);
    }

    private void checkTenant(String tenantId) {
        if (!TenantHelper.isEnable()) {
            return;
        }
        if (TenantConstants.DEFAULT_TENANT_ID.equals(tenantId)) {
            return;
        }
        RemoteTenantVo tenant = remoteTenantService.queryByTenantId(tenantId);
        if (ObjectUtil.isNull(tenant)) {
            log.info("登录租户：{} 不存在.", tenantId);
            throw new TenantException("tenant.not.exists");
        } else if (TenantStatus.DISABLE.getCode().equals(tenant.getStatus())) {
            log.info("登录租户：{} 已被停用.", tenantId);
            throw new TenantException("tenant.blocked");
        } else if (ObjectUtil.isNotNull(tenant.getExpireTime())
            && new Date().after(tenant.getExpireTime())) {
            log.info("登录租户：{} 已超过有效期.", tenantId);
            throw new TenantException("tenant.expired");
        }
    }
}
