package com.ruoyi.job;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 任务调度模块
 *
 * @author Lion Li
 */
@EnableDubbo
@SpringBootApplication
public class RuoYiJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuoYiJobApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  任务调度模块启动成功   ლ(´ڡ`ლ)ﾞ  ");
    }

}
