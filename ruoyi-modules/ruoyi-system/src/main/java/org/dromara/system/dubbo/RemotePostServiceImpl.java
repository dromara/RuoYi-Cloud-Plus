package org.dromara.system.dubbo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.system.api.RemotePostService;
import org.dromara.system.api.model.PostDTO;
import org.dromara.system.domain.SysPost;
import org.dromara.system.domain.vo.SysPostVo;
import org.dromara.system.mapper.SysPostMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 岗位服务
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
@DubboService
public class RemotePostServiceImpl implements RemotePostService {

    private final SysPostMapper postMapper;

    /**
     * 根据岗位 ID 列表查询岗位名称映射关系
     *
     * @param postIds 岗位 ID 列表
     * @return Map，其中 key 为岗位 ID，value 为对应的岗位名称
     */
    @Override
    public Map<Long, String> selectPostNamesByIds(Collection<Long> postIds) {
        if (CollUtil.isEmpty(postIds)) {
            return Collections.emptyMap();
        }
        List<SysPost> list = postMapper.lambda()
            .select(SysPost::getPostId, SysPost::getPostName)
            .in(SysPost::getPostId, postIds)
            .list();
        return StreamUtils.toMap(list, SysPost::getPostId, SysPost::getPostName);
    }

    @Override
    public PostDTO selectById(Long postId) {
        return postId == null ? null : BeanUtil.copyProperties(postMapper.selectById(postId), PostDTO.class);
    }

    @Override
    public List<PostDTO> selectListByIds(Collection<Long> postIds) {
        if (CollUtil.isEmpty(postIds)) {
            return List.of();
        }
        List<SysPostVo> list = postMapper.selectPostList(postMapper.lambda()
            .in(SysPost::getPostId, postIds)
            .orderByAsc(SysPost::getPostSort, SysPost::getPostId)
            .build());
        return BeanUtil.copyToList(list, PostDTO.class);
    }

    @Override
    public List<PostDTO> selectListByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return BeanUtil.copyToList(postMapper.selectPostsByUserId(userId), PostDTO.class);
    }

    @Override
    public List<PostDTO> selectListByDeptId(Long deptId) {
        if (deptId == null) {
            return List.of();
        }
        List<SysPostVo> list = postMapper.selectPostList(postMapper.lambda()
            .eq(SysPost::getDeptId, deptId)
            .orderByAsc(SysPost::getPostSort, SysPost::getPostId)
            .build());
        return BeanUtil.copyToList(list, PostDTO.class);
    }

}
