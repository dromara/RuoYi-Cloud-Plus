package org.dromara.system.api;

/**
 * 配置服务
 *
 * @author Michelle.Chung
 */
public interface RemoteConfigService {

    /**
     * 获取注册开关
     * @param tenantId 租户id
     * @return true开启，false关闭
     */
    boolean selectRegisterEnabled(String tenantId);

}
