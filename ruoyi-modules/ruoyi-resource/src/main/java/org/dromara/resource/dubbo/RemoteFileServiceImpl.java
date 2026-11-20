package org.dromara.resource.dubbo;

import cn.hutool.core.convert.Convert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.oss.client.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.common.oss.model.PutObjectResult;
import org.dromara.resource.api.RemoteFileService;
import org.dromara.resource.api.domain.RemoteFile;
import org.dromara.resource.domain.SysOssExt;
import org.dromara.resource.domain.bo.SysOssBo;
import org.dromara.resource.domain.vo.SysOssVo;
import org.dromara.resource.service.ISysOssService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 文件请求处理
 *
 * @author Lion Li
 */
@Slf4j
@Service
@RequiredArgsConstructor
@DubboService
public class RemoteFileServiceImpl implements RemoteFileService {

    private final ISysOssService sysOssService;

    /**
     * 通过ossId查询对应的url
     *
     * @param ossIds ossId串逗号分隔
     * @return url串逗号分隔
     */
    @Override
    public String selectUrlByIds(String ossIds) {
        return sysOssService.selectUrlByIds(ossIds);
    }

    /**
     * 通过ossId查询列表
     *
     * @param ossIds ossId串逗号分隔
     * @return 列表
     */
    @Override
    public List<RemoteFile> selectByIds(String ossIds) {
        List<SysOssVo> sysOssVos = sysOssService.listByIds(StringUtils.splitTo(ossIds, Convert::toLong));
        return MapstructUtils.convert(sysOssVos, RemoteFile.class);
    }

    @Override
    public RemoteFile selectById(Long ossId) {
        if (ossId == null) {
            return null;
        }
        return MapstructUtils.convert(sysOssService.getById(ossId), RemoteFile.class);
    }

    @Override
    public List<RemoteFile> selectListByIds(Collection<Long> ossIds) {
        if (ossIds == null || ossIds.isEmpty()) {
            return List.of();
        }
        return MapstructUtils.convert(sysOssService.listByIds(ossIds), RemoteFile.class);
    }
}
