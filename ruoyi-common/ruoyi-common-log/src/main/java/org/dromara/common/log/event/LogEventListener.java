package org.dromara.common.log.event;

import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.common.core.utils.BeanCopyUtils;
import org.dromara.system.api.RemoteLogService;
import org.dromara.system.api.domain.bo.RemoteLogininforBo;
import org.dromara.system.api.domain.bo.RemoteOperLogBo;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 异步调用日志服务
 *
 * @author ruoyi
 */
@Component
public class LogEventListener {

    @DubboReference
    private RemoteLogService remoteLogService;

    /**
     * 保存系统日志记录
     */
    @Async
    @EventListener
    public void saveLog(OperLogEvent operLogEvent) {
        RemoteOperLogBo sysOperLog = BeanCopyUtils.copy(operLogEvent, RemoteOperLogBo.class);
        remoteLogService.saveLog(sysOperLog);
    }

    @Async
    @EventListener
    public void saveLogininfor(LogininforEvent logininforEvent) {
        RemoteLogininforBo sysLogininfor = BeanCopyUtils.copy(logininforEvent, RemoteLogininforBo.class);
        remoteLogService.saveLogininfor(sysLogininfor);
    }

}
