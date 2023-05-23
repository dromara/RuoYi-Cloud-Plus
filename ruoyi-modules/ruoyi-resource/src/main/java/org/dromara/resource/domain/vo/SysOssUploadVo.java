package org.dromara.resource.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 上传对象信息
 *
 * @author Michelle.Chung
 */
@Data
public class SysOssUploadVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * URL地址
     */
    private String url;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 对象存储主键
     */
    private String ossId;

}
