package org.dromara.resource.service.impl;

import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.common.oss.client.OssClient;
import org.dromara.resource.domain.vo.SysOssVo;
import org.dromara.common.oss.model.GetObjectResult;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OssContentLookupTest {

    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void resolvesPrivateAndPublicAttachmentUrlsInRequestedOrder(boolean privateBucket) {
        var service = new SysOssServiceImpl(mock());
        var lookup = mock(SysOssServiceImpl.class);
        var file = new SysOssVo();
        file.setService("archive");
        file.setFileName("objects/3");
        file.setUrl("/public/3");
        when(lookup.getById(3L)).thenReturn(file);
        var client = mock(OssClient.class);
        when(client.verifyConfig(any(java.util.function.Function.class))).thenReturn(privateBucket);
        when(client.presignGetUrl("objects/3", java.time.Duration.ofSeconds(120))).thenReturn("/signed/3");
        try (var spring = mockStatic(SpringUtils.class); var factory = mockStatic(OssFactory.class);
             var executor = mockStatic(org.dromara.common.core.utils.ThreadUtils.class)) {
            spring.when(() -> SpringUtils.getAopProxy(service)).thenReturn(lookup);
            factory.when(() -> OssFactory.instance("archive")).thenReturn(client);
            executor.when(() -> org.dromara.common.core.utils.ThreadUtils.virtualSubmitAll(anyCollection()))
                .thenAnswer(call -> {
                    java.util.Collection<java.util.function.Supplier<String>> tasks = call.getArgument(0);
                    var results = new java.util.ArrayList<String>();
                    tasks.forEach(task -> results.add(task.get()));
                    return results;
                });
            assertEquals(privateBucket ? "/signed/3" : "/public/3", service.selectUrlByIds("404,3"));
        }
    }

}
