package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.*;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.*;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysPostVo;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.domain.vo.SysUserExportVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.*;
import org.dromara.system.service.ISysUserService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用户 业务层处理
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysUserServiceImpl implements ISysUserService {

    private final SysUserMapper baseMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final SysPostMapper postMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserPostMapper userPostMapper;

    /**
     * 根据筛选条件分页获取用户列表。
     * 此方法会查询用户基本信息，但不包含用户的角色或岗位详情。
     * 支持通过用户名、状态、手机号等多种条件进行筛选。
     *
     * @param user 包含筛选条件的用户业务对象。例如，可以设置userName进行模糊查询，设置status进行精确匹配。
     * @param pageQuery 分页参数对象，包含页码、每页数量以及排序字段和顺序。
     * @return 封装了用户视图对象列表的分页结果对象 (TableDataInfo<SysUserVo>)。如果查询无结果，则列表为空。
     */
    @Override
    public TableDataInfo<SysUserVo> selectPageUserList(SysUserBo user, PageQuery pageQuery) {
        Page<SysUserVo> page = baseMapper.selectPageUserList(pageQuery.build(), this.buildQueryWrapper(user));
        return TableDataInfo.build(page);
    }

    /**
     * 根据查询条件导出的用户列表。
     * 此方法用于构建用户数据的导出功能，它会根据提供的筛选条件查询用户数据，并转换为特定的导出视图对象。
     * 不进行分页，返回满足条件的所有用户数据。
     *
     * @param user 包含筛选条件的用户业务对象。可用于指定需要导出的用户范围，例如部门、状态等。
     * @return 用户导出视图对象列表 (List<SysUserExportVo>)。如果无满足条件的用户，则返回空列表。
     */
    @Override
    public List<SysUserExportVo> selectUserExportList(SysUserBo user) {
        return baseMapper.selectUserExportList(this.buildQueryWrapper(user));
    }

    private Wrapper<SysUser> buildQueryWrapper(SysUserBo user) {
        Map<String, Object> params = user.getParams();
        QueryWrapper<SysUser> wrapper = Wrappers.query();
        wrapper.eq("u.del_flag", SystemConstants.NORMAL)
            .eq(ObjectUtil.isNotNull(user.getUserId()), "u.user_id", user.getUserId())
            .in(StringUtils.isNotBlank(user.getUserIds()), "u.user_id", StringUtils.splitTo(user.getUserIds(), Convert::toLong))
            .like(StringUtils.isNotBlank(user.getUserName()), "u.user_name", user.getUserName())
            .eq(StringUtils.isNotBlank(user.getStatus()), "u.status", user.getStatus())
            .like(StringUtils.isNotBlank(user.getPhonenumber()), "u.phonenumber", user.getPhonenumber())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                "u.create_time", params.get("beginTime"), params.get("endTime"))
            .and(ObjectUtil.isNotNull(user.getDeptId()), w -> {
                List<SysDept> deptList = deptMapper.selectListByParentId(user.getDeptId());
                List<Long> ids = StreamUtils.toList(deptList, SysDept::getDeptId);
                ids.add(user.getDeptId());
                w.in("u.dept_id", ids);
            }).orderByAsc("u.user_id");
        if (StringUtils.isNotBlank(user.getExcludeUserIds())) {
            wrapper.notIn("u.user_id", StringUtils.splitList(user.getExcludeUserIds()));
        }
        return wrapper;
    }

    /**
     * 根据条件分页查询已分配指定角色的用户列表。
     * 主要用于角色管理模块，查看哪些用户已经被赋予了某个特定角色。
     * 支持通过用户名、状态、手机号等条件进一步筛选已分配的用户。
     *
     * @param user 包含筛选条件的用户业务对象。必须设置roleId以指定查询哪个角色的已分配用户。
     * @param pageQuery 分页参数对象，定义了查询的页码、每页条数等。
     * @return 封装了已分配用户视图对象列表的分页结果 (TableDataInfo<SysUserVo>)。
     */
    @Override
    public TableDataInfo<SysUserVo> selectAllocatedList(SysUserBo user, PageQuery pageQuery) {
        QueryWrapper<SysUser> wrapper = Wrappers.query();
        wrapper.eq("u.del_flag", SystemConstants.NORMAL)
            .eq(ObjectUtil.isNotNull(user.getRoleId()), "r.role_id", user.getRoleId())
            .like(StringUtils.isNotBlank(user.getUserName()), "u.user_name", user.getUserName())
            .eq(StringUtils.isNotBlank(user.getStatus()), "u.status", user.getStatus())
            .like(StringUtils.isNotBlank(user.getPhonenumber()), "u.phonenumber", user.getPhonenumber())
            .orderByAsc("u.user_id");
        Page<SysUserVo> page = baseMapper.selectAllocatedList(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 根据条件分页查询未分配指定角色的用户列表。
     * 主要用于角色管理模块，当需要给某个角色分配用户时，从此列表选择用户。
     * 会排除掉已经分配了该角色的用户。
     * 支持通过用户名、手机号等条件进行筛选。
     *
     * @param user 包含筛选条件的用户业务对象。必须设置roleId以指定查询哪个角色的未分配用户。
     * @param pageQuery 分页参数对象，定义了查询的页码、每页条数等。
     * @return 封装了未分配用户视图对象列表的分页结果 (TableDataInfo<SysUserVo>)。
     */
    @Override
    public TableDataInfo<SysUserVo> selectUnallocatedList(SysUserBo user, PageQuery pageQuery) {
        List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(user.getRoleId());
        QueryWrapper<SysUser> wrapper = Wrappers.query();
        wrapper.eq("u.del_flag", SystemConstants.NORMAL)
            .and(w -> w.ne("r.role_id", user.getRoleId()).or().isNull("r.role_id"))
            .notIn(CollUtil.isNotEmpty(userIds), "u.user_id", userIds)
            .like(StringUtils.isNotBlank(user.getUserName()), "u.user_name", user.getUserName())
            .like(StringUtils.isNotBlank(user.getPhonenumber()), "u.phonenumber", user.getPhonenumber())
            .orderByAsc("u.user_id");
        Page<SysUserVo> page = baseMapper.selectUnallocatedList(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 通过用户名精确查询用户信息。
     * 此方法主要用于登录验证、用户信息获取等场景。
     *
     * @param userName 要查询的用户名。
     * @return 匹配的用户视图对象 (SysUserVo)。如果用户不存在，则返回null。
     */
    @Override
    public SysUserVo selectUserByUserName(String userName) {
        return baseMapper.selectVoOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUserName, userName));
    }

    /**
     * 通过手机号码精确查询用户信息。
     * 可用于通过手机号登录、找回密码或检查手机号是否已被注册等场景。
     *
     * @param phonenumber 要查询的手机号码。
     * @return 匹配的用户视图对象 (SysUserVo)。如果用户不存在，则返回null。
     */
    @Override
    public SysUserVo selectUserByPhonenumber(String phonenumber) {
        return baseMapper.selectVoOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhonenumber, phonenumber));
    }

    /**
     * 通过用户ID查询用户详细信息。
     * 除了用户基本信息外，此方法还会额外查询并填充用户的角色信息。
     *
     * @param userId 要查询的用户ID。
     * @return 包含用户基本信息和角色信息的用户视图对象 (SysUserVo)。如果用户不存在，则返回null。
     */
    @Override
    public SysUserVo selectUserById(Long userId) {
        SysUserVo user = baseMapper.selectVoById(userId);
        if (ObjectUtil.isNull(user)) {
            return user;
        }
        user.setRoles(roleMapper.selectRolesByUserId(user.getUserId()));
        return user;
    }

    /**
     * 根据用户ID列表和可选的部门ID查询用户列表。
     * 此方法用于批量获取指定用户的基本信息（用户ID、用户名、昵称、邮箱、手机号）。
     * 只查询状态为正常的用户。
     *
     * @param userIds 用户ID列表。如果列表为空或null，则可能返回空结果或查询所有用户（取决于具体实现）。
     * @param deptId 可选的部门ID。如果提供，则只在这些用户ID中筛选属于该部门的用户。
     * @return 符合条件的用户视图对象列表 (List<SysUserVo>)。
     */
    @Override
    public List<SysUserVo> selectUserByIds(List<Long> userIds, Long deptId) {
        return baseMapper.selectUserList(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getUserId, SysUser::getUserName, SysUser::getNickName, SysUser::getEmail, SysUser::getPhonenumber)            .eq(SysUser::getStatus, SystemConstants.NORMAL)
            .eq(ObjectUtil.isNotNull(deptId), SysUser::getDeptId, deptId)
            .in(CollUtil.isNotEmpty(userIds), SysUser::getUserId, userIds));
    }

    /**
     * 根据用户ID查询该用户所属的所有角色名称，并以逗号分隔的字符串形式返回。
     * 例如，如果用户属于“管理员”和“普通用户”两个角色，则返回 "管理员,普通用户"。
     *
     * @param userId 用户ID。
     * @return 用户所属角色名称的字符串，角色名之间用逗号分隔。如果用户没有分配任何角色，则返回空字符串。
     */
    @Override
    public String selectUserRoleGroup(Long userId) {
        List<SysRoleVo> list = roleMapper.selectRolesByUserId(userId);
        if (CollUtil.isEmpty(list)) {
            return StringUtils.EMPTY;
        }
        return StreamUtils.join(list, SysRoleVo::getRoleName);
    }

    /**
     * 根据用户ID查询该用户所属的所有岗位名称，并以逗号分隔的字符串形式返回。
     * 例如，如果用户属于“项目经理”和“开发工程师”两个岗位，则返回 "项目经理,开发工程师"。
     *
     * @param userId 用户ID。
     * @return 用户所属岗位名称的字符串，岗位名之间用逗号分隔。如果用户没有分配任何岗位，则返回空字符串。
     */
    @Override
    public String selectUserPostGroup(Long userId) {
        List<SysPostVo> list = postMapper.selectPostsByUserId(userId);
        if (CollUtil.isEmpty(list)) {
            return StringUtils.EMPTY;
        }
        return StreamUtils.join(list, SysPostVo::getPostName);
    }

    /**
     * 校验用户名是否唯一。
     * 在新增或修改用户信息时使用，确保用户名的唯一性。
     * 如果是修改操作，会排除用户自身再进行校验。
     *
     * @param user 包含用户名和可选用户ID的用户业务对象。
     *             - userName: 需要校验的用户名。
     *             - userId: (可选) 如果是修改用户，则传入用户ID，校验时会排除此用户。
     * @return 如果用户名唯一，则返回true；否则返回false。
     */
    @Override
    public boolean checkUserNameUnique(SysUserBo user) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUserName, user.getUserName())
            .ne(ObjectUtil.isNotNull(user.getUserId()), SysUser::getUserId, user.getUserId()));
        return !exist;
    }

    /**
     * 校验手机号码是否唯一。
     * 在新增或修改用户信息时使用，确保手机号码的唯一性。
     * 如果是修改操作，会排除用户自身再进行校验。
     *
     * @param user 包含手机号码和可选用户ID的用户业务对象。
     *             - phonenumber: 需要校验的手机号码。
     *             - userId: (可选) 如果是修改用户，则传入用户ID，校验时会排除此用户。
     * @return 如果手机号码唯一，则返回true；否则返回false。
     */
    @Override
    public boolean checkPhoneUnique(SysUserBo user) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getPhonenumber, user.getPhonenumber())
            .ne(ObjectUtil.isNotNull(user.getUserId()), SysUser::getUserId, user.getUserId()));
        return !exist;
    }

    /**
     * 校验邮箱地址是否唯一。
     * 在新增或修改用户信息时使用，确保邮箱地址的唯一性。
     * 如果是修改操作，会排除用户自身再进行校验。
     *
     * @param user 包含邮箱地址和可选用户ID的用户业务对象。
     *             - email: 需要校验的邮箱地址。
     *             - userId: (可选) 如果是修改用户，则传入用户ID，校验时会排除此用户。
     * @return 如果邮箱地址唯一，则返回true；否则返回false。
     */
    @Override
    public boolean checkEmailUnique(SysUserBo user) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getEmail, user.getEmail())
            .ne(ObjectUtil.isNotNull(user.getUserId()), SysUser::getUserId, user.getUserId()));
        return !exist;
    }

    /**
     * 校验是否允许对指定用户进行操作。
     * 主要用于防止对超级管理员账户执行某些敏感操作（如删除、修改状态等）。
     *
     * @param userId 要检查的用户ID。
     * @throws ServiceException 如果不允许操作（例如，试图操作超级管理员），则抛出此异常。
     */
    @Override
    public void checkUserAllowed(Long userId) {
        if (ObjectUtil.isNotNull(userId) && LoginHelper.isSuperAdmin(userId)) {
            throw new ServiceException("不允许操作超级管理员用户");
        }
    }

    /**
     * 校验当前登录用户是否具有访问指定用户数据的权限。
     * 如果当前用户是超级管理员，则拥有所有权限。
     * 否则，会检查数据范围权限，确保当前用户有权访问目标用户ID的数据。
     *
     * @param userId 目标用户ID，即要检查数据权限的用户。
     * @throws ServiceException 如果没有权限访问该用户数据，则抛出此异常。
     */
    @Override
    public void checkUserDataScope(Long userId) {
        if (ObjectUtil.isNull(userId)) {
            return;
        }
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        if (baseMapper.countUserById(userId) == 0) {
            throw new ServiceException("没有权限访问用户数据！");
        }
    }

    /**
     * 新增用户信息，并建立用户与岗位、角色的关联关系。
     * 此方法是事务性的，如果任何一步失败，整个操作将回滚。
     *
     * @param user 待新增的用户业务对象，包含用户基本信息、岗位ID列表和角色ID列表。
     *             用户的密码应在传入前进行加密处理。
     * @return 返回数据库插入操作影响的行数，通常为1表示成功。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertUser(SysUserBo user) {
        SysUser sysUser = MapstructUtils.convert(user, SysUser.class);
        // 新增用户信息
        int rows = baseMapper.insert(sysUser);
        user.setUserId(sysUser.getUserId());
        // 新增用户岗位关联
        insertUserPost(user, false);
        // 新增用户与角色管理
        insertUserRole(user, false);
        return rows;
    }

    /**
     * 注册新用户。
     * 通常用于系统开放注册的场景。
     * 创建者和更新者ID默认设置为0。
     *
     * @param user 待注册的用户业务对象，包含用户基本信息。密码应已加密。
     * @param tenantId 租户ID，标识用户所属的租户。
     * @return 如果用户成功插入数据库，则返回true；否则返回false。
     */
    @Override
    public boolean registerUser(SysUserBo user, String tenantId) {
        user.setCreateBy(0L);
        user.setUpdateBy(0L);
        SysUser sysUser = MapstructUtils.convert(user, SysUser.class);
        sysUser.setTenantId(tenantId);
        return baseMapper.insert(sysUser) > 0;
    }

    /**
     * 修改用户信息，并更新用户与岗位、角色的关联关系。
     * 此方法是事务性的，会先清除旧的关联关系，然后添加新的关联关系。
     * 会清除用户昵称相关的缓存 (CacheNames.SYS_NICKNAME)。
     *
     * @param user 待修改的用户业务对象，包含用户ID、需要更新的基本信息、岗位ID列表和角色ID列表。
     *             不允许通过此方法修改密码。
     * @return 返回数据库更新操作影响的行数。
     * @throws ServiceException 如果更新失败（例如，影响行数为0），则抛出此异常。
     */
    @Override
    @CacheEvict(cacheNames = CacheNames.SYS_NICKNAME, key = "#user.userId")
    @Transactional(rollbackFor = Exception.class)
    public int updateUser(SysUserBo user) {
        // 新增用户与角色管理
        insertUserRole(user, true);
        // 新增用户与岗位管理
        insertUserPost(user, true);
        SysUser sysUser = MapstructUtils.convert(user, SysUser.class);
        // 防止错误更新后导致的数据误删除
        int flag = baseMapper.updateById(sysUser);
        if (flag < 1) {
            throw new ServiceException("修改用户" + user.getUserName() + "信息失败");
        }
        return flag;
    }

    /**
     * 为用户授权角色。
     * 此方法会先清除用户已有的所有角色关联，然后添加指定的角色关联。
     * 操作是事务性的。
     *
     * @param userId  要授权的用户ID。
     * @param roleIds 要授予用户的角色ID数组。如果为null或空数组，则相当于清空用户所有角色。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertUserAuth(Long userId, Long[] roleIds) {
        insertUserRole(userId, roleIds, true);
    }

    /**
     * 修改用户账户状态（例如，启用或禁用账户）。
     *
     * @param userId 要修改状态的用户ID。
     * @param status 目标账户状态标识字符串（通常来自预定义的常量，如 "0" 代表正常, "1" 代表停用）。
     * @return 返回数据库更新操作影响的行数。
     */
    @Override
    public int updateUserStatus(Long userId, String status) {
        return baseMapper.update(null,
            new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getStatus, status)
                .eq(SysUser::getUserId, userId));
    }

    /**
     * 修改用户个人基本信息。
     * 此方法用于用户自行修改其昵称、手机号、邮箱、性别等信息。
     * 会清除用户昵称相关的缓存 (CacheNames.SYS_NICKNAME)。
     *
     * @param user 包含用户ID及待更新的个人信息的用户业务对象。
     *             只有非null的字段才会被更新。
     * @return 返回数据库更新操作影响的行数。
     */
    @CacheEvict(cacheNames = CacheNames.SYS_NICKNAME, key = "#user.userId")
    @Override
    public int updateUserProfile(SysUserBo user) {
        return baseMapper.update(null,
            new LambdaUpdateWrapper<SysUser>()
                .set(ObjectUtil.isNotNull(user.getNickName()), SysUser::getNickName, user.getNickName())
                .set(SysUser::getPhonenumber, user.getPhonenumber())
                .set(SysUser::getEmail, user.getEmail())
                .set(SysUser::getSex, user.getSex())
                .eq(SysUser::getUserId, user.getUserId()));
    }

    /**
     * 修改用户头像。
     *
     * @param userId 要修改头像的用户ID。
     * @param avatar 新头像的资源ID或标识（具体含义取决于系统中头像的管理方式，通常为文件ID）。
     * @return 如果更新成功（影响行数大于0），则返回true；否则返回false。
     */
    @Override
    public boolean updateUserAvatar(Long userId, Long avatar) {
        return baseMapper.update(null,
            new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getAvatar, avatar)
                .eq(SysUser::getUserId, userId)) > 0;
    }

    /**
     * 重置用户密码。
     * 通常由管理员操作，为用户设置新的密码。
     *
     * @param userId   要重置密码的用户ID。
     * @param password 经过加密处理的新密码。
     * @return 返回数据库更新操作影响的行数。
     */
    @Override
    public int resetUserPwd(Long userId, String password) {
        return baseMapper.update(null,
            new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getPassword, password)
                .eq(SysUser::getUserId, userId));
    }

    /**
     * 新增用户角色信息
     *
     * @param user  用户对象
     * @param clear 清除已存在的关联数据
     */
    private void insertUserRole(SysUserBo user, boolean clear) {
        this.insertUserRole(user.getUserId(), user.getRoleIds(), clear);
    }

    /**
     * 新增用户岗位信息
     *
     * @param user  用户对象
     * @param clear 清除已存在的关联数据
     */
    private void insertUserPost(SysUserBo user, boolean clear) {
        Long[] posts = user.getPostIds();
        if (ArrayUtil.isNotEmpty(posts)) {
            if (clear) {
                // 删除用户与岗位关联
                userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, user.getUserId()));
            }
            // 新增用户与岗位管理
            List<SysUserPost> list = StreamUtils.toList(List.of(posts), postId -> {
                SysUserPost up = new SysUserPost();
                up.setUserId(user.getUserId());
                up.setPostId(postId);
                return up;
            });
            userPostMapper.insertBatch(list);
        }
    }

    /**
     * 新增用户角色信息
     *
     * @param userId  用户ID
     * @param roleIds 角色组
     * @param clear   清除已存在的关联数据
     */
    private void insertUserRole(Long userId, Long[] roleIds, boolean clear) {
        if (ArrayUtil.isNotEmpty(roleIds)) {
            List<Long> roleList = new ArrayList<>(List.of(roleIds));
            if (!LoginHelper.isSuperAdmin(userId)) {
                roleList.remove(SystemConstants.SUPER_ADMIN_ID);
            }
            // 判断是否具有此角色的操作权限
            List<SysRoleVo> roles = roleMapper.selectRoleList(
                new QueryWrapper<SysRole>().in("r.role_id", roleList));
            if (CollUtil.isEmpty(roles)) {
                throw new ServiceException("没有权限访问角色的数据");
            }
            if (clear) {
                // 删除用户与角色关联
                userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
            }
            // 新增用户与角色管理
            List<SysUserRole> list = StreamUtils.toList(roleList, roleId -> {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                return ur;
            });
            userRoleMapper.insertBatch(list);
        }
    }

    /**
     * 通过用户ID删除用户。
     * 此操作会一并删除用户与角色、用户与岗位的关联数据。
     * 操作是事务性的。
     *
     * @param userId 要删除的用户ID。
     * @return 返回数据库删除操作影响的行数。
     * @throws ServiceException 如果删除失败（例如，影响行数为0），则抛出此异常。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserById(Long userId) {
        // 删除用户与角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        // 删除用户与岗位表
        userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, userId));
        // 防止更新失败导致的数据删除
        int flag = baseMapper.deleteById(userId);
        if (flag < 1) {
            throw new ServiceException("删除用户失败!");
        }
        return flag;
    }

    /**
     * 批量删除用户信息。
     * 在执行删除前，会对每个用户进行权限校验（是否允许操作、是否有数据权限）。
     * 此操作会一并删除用户与角色、用户与岗位的关联数据。
     * 操作是事务性的。
     *
     * @param userIds 需要删除的用户ID数组。
     * @return 返回数据库批量删除操作影响的总行数。
     * @throws ServiceException 如果删除过程中任何用户校验失败或数据库操作失败，则抛出此异常。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserByIds(Long[] userIds) {
        for (Long userId : userIds) {
            checkUserAllowed(userId);
            checkUserDataScope(userId);
        }
        List<Long> ids = List.of(userIds);
        // 删除用户与角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, ids));
        // 删除用户与岗位表
        userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().in(SysUserPost::getUserId, ids));
        // 防止更新失败导致的数据删除
        int flag = baseMapper.deleteByIds(ids);
        if (flag < 1) {
            throw new ServiceException("删除用户失败!");
        }
        return flag;
    }

    /**
     * 根据部门ID查询该部门下的所有用户信息。
     * 返回的用户列表按用户ID升序排列。
     *
     * @param deptId 部门ID。
     * @return 属于该部门的用户视图对象列表 (List<SysUserVo>)。如果部门不存在或部门下无用户，则返回空列表。
     */
    @Override
    public List<SysUserVo> selectUserListByDept(Long deptId) {
        LambdaQueryWrapper<SysUser> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysUser::getDeptId, deptId);
        lqw.orderByAsc(SysUser::getUserId);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 根据角色ID列表查询所有拥有这些角色的用户ID。
     *
     * @param roleIds 角色ID列表。
     * @return 拥有指定角色的用户ID列表 (List<Long>)。如果没有任何用户拥有这些角色，则返回空列表。
     */
    @Override
    public List<Long> selectUserIdsByRoleIds(List<Long> roleIds) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getRoleId, roleIds));
        return StreamUtils.toList(userRoles, SysUserRole::getUserId);
    }

    /**
     * 通过用户ID查询用户账户名（登录名）。
     * 结果会被缓存 (CacheNames.SYS_USER_NAME)。
     *
     * @param userId 用户ID。
     * @return 用户账户名。如果用户不存在，可能返回null或空字符串，具体取决于ObjectUtils.notNullGetter的行为。
     */
    @Cacheable(cacheNames = CacheNames.SYS_USER_NAME, key = "#userId")
    @Override
    public String selectUserNameById(Long userId) {
        SysUser sysUser = baseMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getUserName).eq(SysUser::getUserId, userId));
        return ObjectUtils.notNullGetter(sysUser, SysUser::getUserName);
    }

    /**
     * 通过用户ID查询用户昵称。
     * 结果会被缓存 (CacheNames.SYS_NICKNAME)。
     *
     * @param userId 用户ID。
     * @return 用户昵称。如果用户不存在或昵称为空，可能返回null或空字符串，具体取决于ObjectUtils.notNullGetter的行为。
     */
    @Override
    @Cacheable(cacheNames = CacheNames.SYS_NICKNAME, key = "#userId")
    public String selectNicknameById(Long userId) {
        SysUser sysUser = baseMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getNickName).eq(SysUser::getUserId, userId));
        return ObjectUtils.notNullGetter(sysUser, SysUser::getNickName);
    }

    /**
     * 根据用户ID字符串（逗号分隔）查询对应的用户昵称字符串（逗号分隔）。
     * 会调用 {@link #selectNicknameById(Long)} 方法获取每个用户的昵称，该方法带有缓存。
     *
     * @param userIds 逗号分隔的用户ID字符串。例如 "1,2,3"。
     * @return 逗号分隔的用户昵称字符串。如果某个用户ID无效或用户昵称为空，则该昵称不会包含在结果中。
     *         例如，如果ID为1的用户昵称为"张三"，ID为2的昵称为空，ID为3的昵称为"李四"，则返回 "张三,李四"。
     */
    @Override
    public String selectNicknameByIds(String userIds) {
        List<String> list = new ArrayList<>();
        for (Long id : StringUtils.splitTo(userIds, Convert::toLong)) {
            String nickname = SpringUtils.getAopProxy(this).selectNicknameById(id);
            if (StringUtils.isNotBlank(nickname)) {
                list.add(nickname);
            }
        }
        return String.join(StringUtils.SEPARATOR, list);
    }

    /**
     * 通过用户ID查询用户手机号码。
     *
     * @param userId 用户ID。
     * @return 用户手机号码。如果用户不存在或手机号码为空，可能返回null或空字符串，具体取决于ObjectUtils.notNullGetter的行为。
     */
    @Override
    public String selectPhonenumberById(Long userId) {
        SysUser sysUser = baseMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getPhonenumber).eq(SysUser::getUserId, userId));
        return ObjectUtils.notNullGetter(sysUser, SysUser::getPhonenumber);
    }

    /**
     * 通过用户ID查询用户邮箱地址。
     *
     * @param userId 用户ID。
     * @return 用户邮箱地址。如果用户不存在或邮箱地址为空，可能返回null或空字符串，具体取决于ObjectUtils.notNullGetter的行为。
     */
    @Override
    public String selectEmailById(Long userId) {
        SysUser sysUser = baseMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getEmail).eq(SysUser::getUserId, userId));
        return ObjectUtils.notNullGetter(sysUser, SysUser::getEmail);
    }

}
