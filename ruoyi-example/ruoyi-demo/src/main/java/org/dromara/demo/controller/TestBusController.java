package org.dromara.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 测试消息总线
 * <p>
 * 需要在pom引入 ruoyi-api-workflow 模块 并解除下方代码注释
 * 然后提交请假申请即可看到监听器输出日志
 *
 * @author Lion Li
 */
@Slf4j
@RestController
@RequestMapping("/bus")
public class TestBusController {

//    @EventListener(condition = "#processEvent.key.startsWith('leave')")
//    public void processHandler(ProcessEvent processEvent) {
//        log.info(processEvent.toString());
//    }
//
//    @EventListener(condition = "#processTaskEvent.key=='leave1' && #processTaskEvent.taskDefinitionKey=='Activity_14633hx'")
//    public void processTaskHandler(ProcessTaskEvent processTaskEvent) {
//        log.info(processTaskEvent.toString());
//    }

}
