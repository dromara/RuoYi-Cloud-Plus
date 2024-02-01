package org.dromara.common.dubbo.config;

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

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        InetUtils inetUtils = beanFactory.getBean(InetUtils.class);
        String ip = "127.0.0.1";
        InetAddress address = inetUtils.findFirstNonLoopbackAddress();
        if (address != null) {
            if (address instanceof Inet6Address) {
                String ipv6AddressString = address.getHostAddress();
                if (ipv6AddressString.contains("%")) {
                    ipv6AddressString = ipv6AddressString.substring(0, ipv6AddressString.indexOf("%"));
                }
                ip = ipv6AddressString;
            } else {
                ip = inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
            }
        }
        System.setProperty("DUBBO_IP_TO_REGISTRY", ip);
    }
}
