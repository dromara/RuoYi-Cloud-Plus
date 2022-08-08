package com.ruoyi.common.core.constant;

/**
 * 缓存常量信息
 *
 * @author Lion Li
 */
public interface CacheConstants {

    /**
     * 登录用户 redis key
     */
    String LOGIN_TOKEN_KEY = "Authorization:login:token:";

    /**
     * 在线用户 redis key
     */
    String ONLINE_TOKEN_KEY = "online_tokens:";

    /**
     * loginid构造拼接字符串
     */
    String LOGINID_JOIN_CODE = ":";

    /**
     * 密码最大错误次数
     */
    int passwordMaxRetryCount = 5;

    /**
     * 密码锁定时间，默认10（分钟）
     */
    long passwordLockTime = 10;

    /**
     * 登陆错误 redis key
     */
    String LOGIN_ERROR = "login_error:";

    /**
     * 登录错误次数
     */
    Integer LOGIN_ERROR_NUMBER = 5;

    /**
     * 登录错误限制时间(分钟)
     */
    Integer LOGIN_ERROR_LIMIT_TIME = 10;

    /**
     * 验证码 redis key
     */
    String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 参数管理 cache key
     */
    String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    String SYS_DICT_KEY = "sys_dict:";

    /**
     * 登录账户密码错误次数 redis key
     */
    String PWD_ERR_CNT_KEY = "pwd_err_cnt:";
}
