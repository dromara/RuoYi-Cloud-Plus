package org.dromara.workflow.api.domain.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.utils.SpringUtils;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

import java.io.Serial;

/**
 * 流程办理监听
 *
 * @author may
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProcessTaskEvent extends RemoteApplicationEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程定义key
     */
    private String key;

    /**
     * 审批节点key
     */
    private String taskDefinitionKey;

    /**
     * 任务id
     */
    private String taskId;

    /**
     * 业务id
     */
    private String businessKey;

    public ProcessTaskEvent() {
        super(new Object(), SpringUtils.getApplicationName(), DEFAULT_DESTINATION_FACTORY.getDestination(null));
    }
}
