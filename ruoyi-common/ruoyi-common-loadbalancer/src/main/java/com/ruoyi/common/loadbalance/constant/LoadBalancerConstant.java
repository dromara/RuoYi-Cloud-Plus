package com.ruoyi.common.loadbalance.constant;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 常量类
 *
 * @author Lion Li
 */
public class LoadBalancerConstant {

    /**
     * 获取服务host
     * 默认自动获取
     */
    public static String getHost() {
        String host = "127.0.0.1";
        try {
            // 如需自定义ip可修改此处
            String address = InetAddress.getLocalHost().getHostAddress();
            if (address != null) {
                host = address;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return host;
    }
}
