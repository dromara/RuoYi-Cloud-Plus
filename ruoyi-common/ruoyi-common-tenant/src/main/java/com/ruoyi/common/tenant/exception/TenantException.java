package com.ruoyi.common.tenant.exception;


import com.ruoyi.common.core.exception.base.BaseException;

/**
 * 租户异常类
 *
 * @author Lion Li
 */
public class TenantException extends BaseException {

    private static final long serialVersionUID = 1L;

    public TenantException(String code, Object... args) {
        super("tenant", code, args, null);
    }
}
