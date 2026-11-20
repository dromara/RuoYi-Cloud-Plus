package org.dromara.resource.api;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.resource.api.domain.RemoteFile;

import java.util.Collection;
import java.util.List;

/**
 * 文件服务
 *
 * @author Lion Li
 */
public interface RemoteFileService {

    /**
     * 通过ossId查询对应的url
     *
     * @param ossIds ossId串逗号分隔
     * @return url串逗号分隔
     */
    String selectUrlByIds(String ossIds);

    /**
     * 通过ossId查询列表
     *
     * @param ossIds ossId串逗号分隔
     * @return 列表
     */
    List<RemoteFile> selectByIds(String ossIds);

    /**
     * 根据文件 ID 查询文件信息。
     *
     * @param ossId 文件 ID
     * @return 文件信息
     */
    RemoteFile selectById(Long ossId);

    /**
     * 根据文件 ID 集合查询文件信息。
     *
     * @param ossIds 文件 ID 集合
     * @return 存在的文件信息列表
     */
    List<RemoteFile> selectListByIds(Collection<Long> ossIds);
}
