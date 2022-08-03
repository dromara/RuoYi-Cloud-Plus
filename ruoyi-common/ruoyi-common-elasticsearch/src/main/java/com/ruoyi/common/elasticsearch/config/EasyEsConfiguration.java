package com.ruoyi.common.elasticsearch.config;

import cn.easyes.starter.register.EsMapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * easy-es 配置
 *
 * @author Lion Li
 */
@AutoConfiguration
@EsMapperScan("com.ruoyi.**.esmapper")
public class EasyEsConfiguration {

}
