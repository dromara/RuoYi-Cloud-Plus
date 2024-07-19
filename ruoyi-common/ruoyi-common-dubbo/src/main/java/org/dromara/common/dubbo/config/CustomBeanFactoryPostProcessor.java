package org.dromara.common.dubbo.config;

import org.dromara.common.core.utils.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.core.Ordered;

import java.net.Inet6Address;
import java.net.InetAddress;

/**
 * dubbo自定义IP注入(避免IP不正确问题)
 *
 * @author Lion Li
 */
public class CustomBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered {

    /**
     * 获取该 BeanFactoryPostProcessor 的顺序，确保它在容器初始化过程中具有最高优先级
     *
     * @return 优先级顺序值，越小优先级越高
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 在 Spring 容器初始化过程中对 Bean 工厂进行后置处理
     *
     * @param beanFactory 可配置的 Bean 工厂
     * @throws BeansException 如果在处理过程中发生错误
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String property = System.getProperty("DUBBO_IP_TO_REGISTRY");
        if (StringUtils.isNotBlank(property)) {
            return;
        }
        // 获取 InetUtils bean，用于获取 IP 地址
        InetUtils inetUtils = beanFactory.getBean(InetUtils.class);
        String ip = "127.0.0.1";
        // 获取第一个非回环地址
        InetAddress address = inetUtils.findFirstNonLoopbackAddress();
        if (address != null) {
            if (address instanceof Inet6Address) {
                // 处理 IPv6 地址
                String ipv6AddressString = address.getHostAddress();
                if (ipv6AddressString.contains("%")) {
                    // 去掉可能存在的范围 ID
                    ipv6AddressString = ipv6AddressString.substring(0, ipv6AddressString.indexOf("%"));
                }
                ip = ipv6AddressString;
            } else {
                // 处理 IPv4 地址
                ip = inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
            }
        }
        // 设置系统属性 DUBBO_IP_TO_REGISTRY 为获取到的 IP 地址
        System.setProperty("DUBBO_IP_TO_REGISTRY", ip);
    }
}
