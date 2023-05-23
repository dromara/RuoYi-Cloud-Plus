package org.dromara.common.core.exception;

import java.io.Serial;

/**
 * 检查异常
 *
 * @author ruoyi
 */
public class CheckedException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public CheckedException(String message) {
        super(message);
    }

    public CheckedException(Throwable cause) {
        super(cause);
    }

    public CheckedException(String message, Throwable cause) {
        super(message, cause);
    }

    public CheckedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
