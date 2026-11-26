package org.dromara.common.dubbo;

import org.apache.dubbo.common.constants.CommonConstants;
import org.dromara.common.dubbo.config.CustomBeanFactoryPostProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;

import java.net.Inet6Address;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DubboRegistryIpTest {
    @Test
    void preservesConfiguredAddressAndNormalizesDiscoveredIpv6() throws Exception {
        String key = CommonConstants.DubboProperty.DUBBO_IP_TO_REGISTRY;
        String old = System.getProperty(key);
        try {
            var processor = new CustomBeanFactoryPostProcessor();
            processor.setEnvironment(new MockEnvironment());
            var factory = new DefaultListableBeanFactory();
            var address = Inet6Address.getByAddress(null, new byte[16], 4);
            try (var utilities = mockConstruction(InetUtils.class, (network, context) ->
                when(network.findFirstNonLoopbackAddress()).thenReturn(address))) {
                System.setProperty(key, "192.0.2.10");
                processor.postProcessBeanFactory(factory);
                assertEquals("192.0.2.10", System.getProperty(key));
                assertTrue(utilities.constructed().isEmpty());

                System.clearProperty(key);
                processor.postProcessBeanFactory(factory);
                assertEquals(address.getHostAddress().split("%")[0], System.getProperty(key));
                assertEquals(1, utilities.constructed().size());
                assertEquals(Ordered.HIGHEST_PRECEDENCE, processor.getOrder());
            }
        } finally {
            if (old == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, old);
            }
        }
    }
}
