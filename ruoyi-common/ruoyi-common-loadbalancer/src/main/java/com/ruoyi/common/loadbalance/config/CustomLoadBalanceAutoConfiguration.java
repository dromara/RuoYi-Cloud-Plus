package com.ruoyi.common.loadbalance.config;

import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.PropertySource;

/**
 * 自定义负载均衡自动配置
 *
 * @author Lion Li
 */
@PropertySource(value = "classpath:loadbalance.properties", encoding = "UTF-8")
@LoadBalancerClients(defaultConfiguration = CustomLoadBalanceClientConfiguration.class)
public class CustomLoadBalanceAutoConfiguration {

}
