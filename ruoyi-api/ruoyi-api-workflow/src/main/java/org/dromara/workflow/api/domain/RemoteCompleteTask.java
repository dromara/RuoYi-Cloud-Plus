package org.dromara.workflow.api.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 办理任务请求对象
 *
 * @author may
 */
@Data
public class RemoteCompleteTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务id
     */
    private Long taskId;

    /**
     * 附件id
     */
    private String fileId;

    /**
     * 抄送人员
     */
    private List<RemoteFlowCopy> flowCopyList;

    /**
     * 消息类型
     */
    private List<String> messageType;

    /**
     * 办理意见
     */
    private String message;

    /**
     * 消息通知
     */
    private String notice;

    /**
     * 办理人(可不填 用于覆盖当前节点办理人)
     */
    private String handler;

    /**
     * 流程变量
     */
    private Map<String, Object> variables;

    /**
     * Retains a writable copy of the supplied workflow variables.
     *
     * @param variables workflow variables
     */
    public void setVariables(Map<String, Object> variables) {
        this.variables = variables == null ? null : new HashMap<>(variables);
    }

    /**
     * 扩展变量(此处为逗号分隔的ossId)
     */
    private String ext;

    public Map<String, Object> getVariables() {
        if (variables == null) {
            variables = new HashMap<>(16);
            return variables;
        }
        variables.entrySet().removeIf(entry -> Objects.isNull(entry.getValue()));
        return variables;
    }

}
