package com.ruoyi.common.core.constant;

/**
 * 缓存的key 常量
 *
 * @author Lion Li
 */
public interface CacheConstants {
    /**
     * 缓存有效期，默认720（分钟）
     */
    long EXPIRATION = 720;

    /**
     * 缓存刷新时间，默认120（分钟）
     */
    long REFRESH_TIME = 120;

    /**
     * 权限缓存前缀
     */
    String LOGIN_TOKEN_KEY = "login_tokens:";
}
