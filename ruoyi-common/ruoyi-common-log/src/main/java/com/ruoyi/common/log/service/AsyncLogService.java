package com.ruoyi.common.log.service;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.system.api.RemoteLogService;
import com.ruoyi.system.api.domain.SysOperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步调用日志服务
 *
 * @author ruoyi
 */
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class AsyncLogService {

    private final RemoteLogService remoteLogService;

    /**
     * 保存系统日志记录
     */
    @Async
    public void saveSysLog(SysOperLog sysOperLog) {
        remoteLogService.saveLog(sysOperLog, SecurityConstants.INNER);
    }
}
