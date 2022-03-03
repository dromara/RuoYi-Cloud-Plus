package com.ruoyi.common.idempotent.config;

import com.ruoyi.common.idempotent.aspectj.RepeatSubmitAspect;
import com.ruoyi.common.redis.config.RedisConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 幂等功能配置
 *
 * @author Lion Li
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(RedisConfiguration.class)
public class IdempotentAutoConfiguration {

	@Bean
	public RepeatSubmitAspect repeatSubmitAspect() {
		return new RepeatSubmitAspect();
	}

}
