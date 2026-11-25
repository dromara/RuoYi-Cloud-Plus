package org.dromara.resource.controller;

import org.dromara.resource.domain.vo.SysOssUploadVo;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.resource.domain.SysOssExt;
import org.dromara.resource.domain.vo.SysOssVo;
import org.dromara.resource.service.ISysOssService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OssUploadResponseTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsStoredFileAndPassesOptionalExtension(boolean withExtension) {
        var service = mock(ISysOssService.class);
        var file = new MockMultipartFile("file", new byte[]{1});
        var saved = new SysOssVo();
        saved.setUrl("/files/key");
        saved.setOriginalName("report.pdf");
        saved.setOssId(9007199254740993L);
        when(service.upload(eq(file), nullable(SysOssExt.class))).thenReturn(saved);
        try (var beans = mockStatic(SpringUtil.class)) {
            beans.when(() -> SpringUtil.getBean(JsonMapper.class)).thenReturn(JsonMapper.builder().build());
            String json = withExtension ? "{\"bizType\":\"report\",\"refId\":\"21\"}" : null;
            var response = new SysOssController(service).upload(file, json).getData();
            assertEquals("/files/key", response.getUrl());
            assertEquals("report.pdf", response.getFileName());
            assertEquals("9007199254740993", response.getOssId());
            var extension = ArgumentCaptor.forClass(SysOssExt.class);
            verify(service).upload(eq(file), extension.capture());
            if (withExtension) {
                assertEquals("report", extension.getValue().getBizType());
                assertEquals("21", extension.getValue().getRefId());
            } else {
                assertNull(extension.getValue());
            }
        }
    }
}
