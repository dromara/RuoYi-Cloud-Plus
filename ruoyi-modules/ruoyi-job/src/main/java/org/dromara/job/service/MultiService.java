package org.dromara.job.service;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.system.api.RemoteUserService;
import org.dromara.system.api.domain.bo.RemoteUserBo;
import org.dromara.system.api.model.LoginUser;
import org.springframework.stereotype.Service;

/**
 * XxlJob 多服务调用
 *
 * @author Lion Li
 */
@Slf4j
@Service
public class MultiService {

    @DubboReference
    private RemoteUserService remoteUserService;

    /**
     * 多服务调用
     */
    @GlobalTransactional(rollbackFor = Exception.class)
    @XxlJob("multiServiceHandler")
    public void multiServiceHandler() throws Exception {
        LoginUser admin = remoteUserService.getUserInfo("admin");
        XxlJobHelper.log("XXL-JOB, multiServiceHandler result: {}", admin.toString());
        RemoteUserBo remoteUserBo = new RemoteUserBo();
        remoteUserBo.setUserName("test");
        remoteUserBo.setNickName("test");
        remoteUserService.registerUserInfo(remoteUserBo);
    }

}
