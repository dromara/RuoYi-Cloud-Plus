package com.ruoyi.file.api;

import com.ruoyi.file.api.domain.SysFile;

/**
 * 文件服务
 *
 * @author Lion Li
 */
public interface RemoteFileService {

    /**
     * 上传文件
     *
     * @param file 文件信息
     * @return 结果
     */
    SysFile upload(String name, String originalFilename, String contentType, byte[] file);
}
