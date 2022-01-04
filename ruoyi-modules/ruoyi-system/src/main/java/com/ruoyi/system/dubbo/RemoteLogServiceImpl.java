package com.ruoyi.system.dubbo;

import com.ruoyi.system.api.RemoteLogService;
import com.ruoyi.system.api.domain.SysLogininfor;
import com.ruoyi.system.api.domain.SysOperLog;
import com.ruoyi.system.service.ISysLogininforService;
import com.ruoyi.system.service.ISysOperLogService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 操作日志记录
 *
 * @author Lion Li
 */
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
@DubboService
public class RemoteLogServiceImpl implements RemoteLogService {

    private final ISysOperLogService operLogService;
    private final ISysLogininforService logininforService;

    @Override
    public Boolean saveLog(SysOperLog sysOperLog, String source){
        return operLogService.insertOperlog(sysOperLog) > 0;
    }

    @Override
    public Boolean saveLogininfor(SysLogininfor sysLogininfor, String source){
        return logininforService.insertLogininfor(sysLogininfor) > 0;
    }
}
