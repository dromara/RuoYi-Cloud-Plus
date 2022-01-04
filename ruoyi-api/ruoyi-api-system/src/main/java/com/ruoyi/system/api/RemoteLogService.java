package com.ruoyi.system.api;

import com.ruoyi.system.api.domain.SysLogininfor;
import com.ruoyi.system.api.domain.SysOperLog;

/**
 * 日志服务
 *
 * @author Lion Li
 */
public interface RemoteLogService {

    /**
     * 保存系统日志
     *
     * @param sysOperLog 日志实体
     * @param source     请求来源
     * @return 结果
     */
    Boolean saveLog(SysOperLog sysOperLog, String source);

    /**
     * 保存访问记录
     *
     * @param sysLogininfor 访问实体
     * @param source        请求来源
     * @return 结果
     */
    Boolean saveLogininfor(SysLogininfor sysLogininfor, String source);
}
