package com.ruoyi.file.dubbo;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.file.FileUtils;
import com.ruoyi.file.api.RemoteFileService;
import com.ruoyi.file.api.domain.SysFile;
import com.ruoyi.file.service.ISysFileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

/**
 * 文件请求处理
 *
 * @author ruoyi
 */
@Slf4j
@Service
@DubboService
public class RemoteFileServiceImpl implements RemoteFileService {

    @Autowired
    private ISysFileService sysFileService;

    /**
     * 文件上传请求
     */
    @Override
    public SysFile upload(String name, String originalFilename, String contentType, byte[] file) {
        MultipartFile multipartFile = getMultipartFile(name, originalFilename, contentType, file);
        try {
            // 上传并返回访问地址
            String url = sysFileService.uploadFile(multipartFile);
            SysFile sysFile = new SysFile();
            sysFile.setName(FileUtils.getName(url));
            sysFile.setUrl(url);
            return sysFile;
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new ServiceException("上传文件失败");
        }
    }

    private MultipartFile getMultipartFile(String name, String originalFilename, String contentType, byte[] file) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getOriginalFilename() {
                return originalFilename;
            }

            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public boolean isEmpty() {
                return getSize() == 0;
            }

            @Override
            public long getSize() {
                return file.length;
            }

            @Override
            public byte[] getBytes() throws IOException {
                return file;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(file);
            }

            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException {
                OutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(dest);
                    outputStream.write(file);
                } finally {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                }
            }
        };
    }
}
