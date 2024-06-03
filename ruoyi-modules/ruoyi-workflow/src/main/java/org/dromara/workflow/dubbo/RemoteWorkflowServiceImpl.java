package org.dromara.workflow.dubbo;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.workflow.api.domain.RemoteWorkflowService;
import org.dromara.workflow.service.IActHiProcinstService;
import org.dromara.workflow.service.WorkflowService;

import java.util.List;
import java.util.Map;

/**
 * RemoteWorkflowServiceImpl
 *
 * @Author ZETA
 * @Date 2024/6/3
 */
@DubboService
@RequiredArgsConstructor
public class RemoteWorkflowServiceImpl implements RemoteWorkflowService {

    private final WorkflowService workflowService;
    private final IActHiProcinstService actHiProcinstService;

    @Override
    public boolean deleteRunAndHisInstance(List<String> businessKeys) {
        return workflowService.deleteRunAndHisInstance(businessKeys);
    }

    @Override
    public String getBusinessStatusByTaskId(String taskId) {
        return workflowService.getBusinessStatusByTaskId(taskId);
    }

    @Override
    public String getBusinessStatus(String businessKey) {
        return workflowService.getBusinessStatus(businessKey);
    }

    @Override
    public void setVariable(String taskId, String variableName, Object value) {
        workflowService.setVariable(taskId, variableName, value);
    }

    @Override
    public void setVariables(String taskId, Map<String, Object> variables) {
        workflowService.setVariables(taskId, variables);
    }

    @Override
    public void setVariableLocal(String taskId, String variableName, Object value) {
        workflowService.setVariableLocal(taskId, variableName, value);
    }

    @Override
    public void setVariablesLocal(String taskId, Map<String, Object> variables) {
        workflowService.setVariablesLocal(taskId, variables);
    }

    /**
     * 按照业务id查询流程实例id
     *
     * @param businessKey 业务id
     * @return 结果
     */
    @Override
    public String getInstanceIdByBusinessKey(String businessKey) {
        return workflowService.getInstanceIdByBusinessKey(businessKey);
    }
}
