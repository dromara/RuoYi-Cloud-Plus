package com.ruoyi.resource.dubbo;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.oss.entity.UploadResult;
import com.ruoyi.common.oss.factory.OssFactory;
import com.ruoyi.common.oss.service.IOssStrategy;
import com.ruoyi.resource.api.RemoteFileService;
import com.ruoyi.resource.api.domain.SysFile;
import com.ruoyi.resource.domain.SysOss;
import com.ruoyi.resource.mapper.SysOssMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文件请求处理
 *
 * @author Lion Li
 */
@Slf4j
@Service
@DubboService
public class RemoteFileServiceImpl implements RemoteFileService {

    @Autowired
    private SysOssMapper sysOssMapper;

    /**
     * 文件上传请求
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SysFile upload(String name, String originalFilename, String contentType, byte[] file) throws ServiceException {
        try {
            String suffix = StringUtils.substring(originalFilename, originalFilename.lastIndexOf("."), originalFilename.length());
            IOssStrategy storage = OssFactory.instance();
            UploadResult uploadResult = storage.uploadSuffix(file, suffix, contentType);
            // 保存文件信息
            SysOss oss = new SysOss();
            oss.setUrl(uploadResult.getUrl());
            oss.setFileSuffix(suffix);
            oss.setFileName(uploadResult.getFilename());
            oss.setOriginalName(originalFilename);
            oss.setService(storage.getServiceType().getValue());
            sysOssMapper.insert(oss);
            SysFile sysFile = new SysFile();
            sysFile.setName(uploadResult.getFilename());
            sysFile.setUrl(uploadResult.getUrl());
            return sysFile;
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new ServiceException("上传文件失败");
        }
    }

}
