package org.dromara.resource.api.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件信息
 *
 * @author ruoyi
 */
@Data
public class RemoteSms implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 配置标识名 如未配置取对应渠道名例如 Alibaba
     */
    private String configId;

    /**
     * 厂商原返回体
     * <p>
     * 可自行转换为 SDK 对应的 SendSmsResponse
     */
    private String response;

}
