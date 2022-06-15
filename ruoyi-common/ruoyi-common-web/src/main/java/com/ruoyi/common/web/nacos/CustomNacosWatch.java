package com.ruoyi.common.web.nacos;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.cloud.nacos.discovery.NacosWatch;

/**
 * 自定义 nacos 监听
 *
 * @author Lion Li
 */
public class CustomNacosWatch extends NacosWatch {

    public CustomNacosWatch(NacosServiceManager nacosServiceManager, NacosDiscoveryProperties properties) {
        super(nacosServiceManager, properties);
    }

    /**
     * 重写解决与 Undertow 关闭顺序冲突导致报错问题
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
