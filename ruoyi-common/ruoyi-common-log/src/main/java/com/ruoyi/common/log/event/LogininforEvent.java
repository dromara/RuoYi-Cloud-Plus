package com.ruoyi.common.log.event;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录事件
 *
 * @author Lion Li
 */

@Data
public class LogininforEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户账号
     */
    private String userName;

    /**
     * 登录状态 0成功 1失败
     */
    private String status;

    /**
     * ip地址
     */
    private String ipaddr;

    /**
     * 提示消息
     */
    private String msg;

}
