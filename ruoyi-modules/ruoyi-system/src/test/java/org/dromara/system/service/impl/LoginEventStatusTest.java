package org.dromara.system.service.impl;

import org.dromara.common.core.constant.Constants;
import org.dromara.common.log.event.LoginInfoEvent;
import org.dromara.system.domain.bo.SysLoginInfoBo;
import org.dromara.system.domain.vo.SysClientVo;
import org.dromara.system.service.ISysClientService;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginEventStatusTest {

    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void loginPaginationPreservesTimeRangeAndExplicitSort(boolean explicit) {
        var mapper = mock(org.dromara.system.mapper.SysLoginInfoMapper.class);
        var type = org.dromara.system.domain.SysLoginInfo.class;
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
            new com.baomidou.mybatisplus.core.MybatisConfiguration(), "history"), type);
        when(mapper.currentModelClass()).thenReturn(type);
        var query = new org.dromara.common.mybatis.core.mapper.LambdaCrudChainWrapper<
            org.dromara.system.domain.SysLoginInfo, org.dromara.system.domain.vo.SysLoginInfoVo>(mapper);
        when(mapper.lambda()).thenReturn(query);
        var page = new org.dromara.common.mybatis.core.page.PageQuery(5, 1);
        if (explicit) {
            page.setOrderByColumn("loginTime");
            page.setIsAsc("asc");
        }
        var data = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<org.dromara.system.domain.vo.SysLoginInfoVo>();
        when(mapper.selectVoPage(any(), any())).thenReturn(data);
        var bo = new SysLoginInfoBo();
        bo.getParams().put("beginTime", "2026-01-01");
        bo.getParams().put("endTime", "2026-02-01");
        new SysLoginInfoServiceImpl(mapper).selectPageLoginInfoList(bo, page);
        org.mockito.ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.dromara.system.domain.SysLoginInfo>> wrapper = ArgumentCaptor.captor();
        verify(mapper).selectVoPage(any(), wrapper.capture());
        String sql = wrapper.getValue().getSqlSegment();
        assertTrue(sql.contains("login_time BETWEEN"), sql);
        assertEquals(!explicit, sql.contains("info_id DESC"));
    }

    @Test
    void clearsLoginHistoryAndUnlocksOnlyNamedAccount() {
        var mapper = mock(org.dromara.system.mapper.SysLoginInfoMapper.class, RETURNS_DEEP_STUBS);
        var query = mapper.lambda();
        var service = new SysLoginInfoServiceImpl(mapper);
        var controller = new org.dromara.system.controller.monitor.SysLoginInfoController(service);
        assertTrue(org.dromara.common.core.domain.R.isSuccess(controller.clean()));
        verify(query).delete();
        String key = org.dromara.common.core.constant.CacheNames.PWD_ERR_CNT_KEY + "alice";
        try (var beans = mockStatic(cn.hutool.extra.spring.SpringUtil.class);
             var cache = mockStatic(org.dromara.common.redis.utils.RedisUtils.class)) {
            cache.when(() -> org.dromara.common.redis.utils.RedisUtils.hasKey(key)).thenReturn(true, false);
            controller.unlock("alice");
            controller.unlock("alice");
            cache.verify(() -> org.dromara.common.redis.utils.RedisUtils.deleteObject(key));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {Constants.LOGIN_SUCCESS, Constants.LOGOUT, Constants.REGISTER, Constants.LOGIN_FAIL})
    void recordsOutcomeAndResolvedClient(String status) {
        var clients = mock(org.dromara.system.api.RemoteClientService.class);
        var service = new org.dromara.common.log.event.LogEventListener();
        var logs = mock(org.dromara.system.api.RemoteLogService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "remoteClientService", clients);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "remoteLogService", logs);
        var client = new org.dromara.system.api.domain.vo.RemoteClientVo();
        client.setClientKey("desktop");
        client.setDeviceType("pc");
        when(clients.queryByClientId("client-8")).thenReturn(client);
        var event = new LoginInfoEvent();
        event.setUsername("alice");
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader(org.dromara.common.satoken.utils.LoginHelper.CLIENT_KEY, "client-8");
        event.setStatus(status);
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/123.0.0.0 Safari/537.36");
        try (var servlet = mockStatic(org.dromara.common.core.utils.ServletUtils.class);
             var address = mockStatic(org.dromara.common.core.utils.ip.AddressUtils.class)) {
            servlet.when(org.dromara.common.core.utils.ServletUtils::getRequest).thenReturn(request);
            service.saveLoginInfo(event);
        }
        var captured = ArgumentCaptor.forClass(org.dromara.system.api.domain.bo.RemoteLoginInfoBo.class);
        verify(logs).saveLoginInfo(captured.capture());
        var row = captured.getValue();
        assertEquals(Constants.LOGIN_FAIL.equals(status) ? Constants.FAIL : Constants.SUCCESS, row.getStatus());
        assertEquals("desktop", row.getClientKey());
        assertEquals("pc", row.getDeviceType());
        assertTrue(row.getBrowser().contains("Chrome"));
        assertTrue(row.getOs().contains("Windows"));
    }
}
