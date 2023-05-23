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
    private Boolean isSuccess;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 实际响应体
     * <p>
     * 可自行转换为 SDK 对应的 SendSmsResponse
     */
    private String response;

}
