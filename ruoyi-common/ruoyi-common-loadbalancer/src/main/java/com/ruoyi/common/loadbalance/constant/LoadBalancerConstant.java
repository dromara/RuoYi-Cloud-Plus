package com.ruoyi.common.loadbalance.constant;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 常量类
 *
 * @author Lion Li
 */
@Slf4j
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
        log.debug("[LoadBalancer] - 本机IP地址: {}", host);
        return host;
    }
}
