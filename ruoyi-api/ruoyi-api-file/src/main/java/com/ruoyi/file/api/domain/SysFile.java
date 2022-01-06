package com.ruoyi.file.api.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 文件信息
 *
 * @author ruoyi
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class SysFile {
    /**
     * 文件名称
     */
    private String name;

    /**
     * 文件地址
     */
    private String url;

}
