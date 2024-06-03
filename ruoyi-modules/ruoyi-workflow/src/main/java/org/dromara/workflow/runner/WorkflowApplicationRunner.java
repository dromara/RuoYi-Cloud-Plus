package org.dromara.workflow.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.resource.api.RemoteFileService;
import org.dromara.system.api.RemoteUserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 初始化 dubbo 接口生成接口代理 不然无法直接用 SpringUtils 注入 dubbo 接口使用
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class WorkflowApplicationRunner implements ApplicationRunner {

    @DubboReference
    private RemoteUserService remoteUserService;
    @DubboReference
    private RemoteFileService remoteFileService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
    }

}
