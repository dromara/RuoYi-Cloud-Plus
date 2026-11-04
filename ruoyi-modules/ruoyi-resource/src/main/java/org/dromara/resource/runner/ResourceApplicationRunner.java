package org.dromara.resource.runner;

import lombok.RequiredArgsConstructor;
import org.dromara.resource.service.ISysOssConfigService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 初始化 system 模块对应业务数据
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Component
public class ResourceApplicationRunner implements ApplicationRunner {

    private final ISysOssConfigService ossConfigService;

    /**
     * 应用启动后初始化 OSS 配置缓存。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        ossConfigService.init();
    }

}
