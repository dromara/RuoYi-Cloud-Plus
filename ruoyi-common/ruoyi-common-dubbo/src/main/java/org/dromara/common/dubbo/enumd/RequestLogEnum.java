package org.dromara.common.dubbo.enumd;

import lombok.AllArgsConstructor;

/**
 * 请求日志泛型
 *
 * @author Lion Li
 */
@AllArgsConstructor
public enum RequestLogEnum {

    /**
     * info 基础信息 param 参数信息 full 全部
     */
    INFO, PARAM, FULL;

}
