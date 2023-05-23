package org.dromara.system.dubbo;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.system.api.RemoteLogService;
import org.dromara.system.api.domain.bo.RemoteLogininforBo;
import org.dromara.system.api.domain.bo.RemoteOperLogBo;
import org.dromara.system.domain.bo.SysLogininforBo;
import org.dromara.system.domain.bo.SysOperLogBo;
import org.dromara.system.service.ISysLogininforService;
import org.dromara.system.service.ISysOperLogService;
import org.springframework.stereotype.Service;

/**
 * 操作日志记录
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
@DubboService
public class RemoteLogServiceImpl implements RemoteLogService {

    private final ISysOperLogService operLogService;
    private final ISysLogininforService logininforService;

    @Override
    public void saveLog(RemoteOperLogBo remoteOperLogBo) {
        SysOperLogBo sysOperLogBo = MapstructUtils.convert(remoteOperLogBo, SysOperLogBo.class);
        operLogService.insertOperlog(sysOperLogBo);
    }

    @Override
    public void saveLogininfor(RemoteLogininforBo remoteLogininforBo) {
        SysLogininforBo sysLogininforBo = MapstructUtils.convert(remoteLogininforBo, SysLogininforBo.class);
        logininforService.insertLogininfor(sysLogininforBo);
    }
}
