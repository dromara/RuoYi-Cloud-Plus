package org.dromara.workflow.flowable.cmd;

import cn.hutool.core.collection.CollUtil;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.resource.api.RemoteFileService;
import org.dromara.resource.api.domain.RemoteFile;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.AttachmentEntity;
import org.flowable.engine.impl.persistence.entity.AttachmentEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;

import java.util.Date;
import java.util.List;

/**
 * 附件上传
 *
 * @author may
 */
public class AttachmentCmd implements Command<Boolean> {

    private final String fileId;

    private final String taskId;

    private final String processInstanceId;

    private final RemoteFileService remoteFileService;

    public AttachmentCmd(String fileId, String taskId, String processInstanceId,
                         RemoteFileService remoteFileService) {
        this.fileId = fileId;
        this.taskId = taskId;
        this.processInstanceId = processInstanceId;
        this.remoteFileService = remoteFileService;
    }

    @Override
    public Boolean execute(CommandContext commandContext) {
        try {
            if (StringUtils.isNotBlank(fileId)) {
                List<RemoteFile> ossList = remoteFileService.selectByIds(fileId);
                if (CollUtil.isNotEmpty(ossList)) {
                    for (RemoteFile oss : ossList) {
                        AttachmentEntityManager attachmentEntityManager = CommandContextUtil.getAttachmentEntityManager();
                        AttachmentEntity attachmentEntity = attachmentEntityManager.create();
                        attachmentEntity.setRevision(1);
                        attachmentEntity.setUserId(LoginHelper.getUserId().toString());
                        attachmentEntity.setName(oss.getOriginalName());
                        attachmentEntity.setDescription(oss.getOriginalName());
                        attachmentEntity.setType(oss.getFileSuffix());
                        attachmentEntity.setTaskId(taskId);
                        attachmentEntity.setProcessInstanceId(processInstanceId);
                        attachmentEntity.setContentId(oss.getOssId().toString());
                        attachmentEntity.setTime(new Date());
                        attachmentEntityManager.insert(attachmentEntity);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
