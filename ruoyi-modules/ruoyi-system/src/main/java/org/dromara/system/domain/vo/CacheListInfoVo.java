package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 缓存监控列表信息
 *
 * @author Michelle.Chung
 */
@Data
public class CacheListInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Properties info;

    private Long dbSize;

    private List<Map<String, String>> commandStats;

}
