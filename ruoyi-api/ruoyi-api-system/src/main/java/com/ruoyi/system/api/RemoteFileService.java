package com.ruoyi.system.api;

import com.ruoyi.system.api.domain.SysFile;
import org.springframework.web.multipart.MultipartFile;

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
    SysFile upload(MultipartFile file);
}
