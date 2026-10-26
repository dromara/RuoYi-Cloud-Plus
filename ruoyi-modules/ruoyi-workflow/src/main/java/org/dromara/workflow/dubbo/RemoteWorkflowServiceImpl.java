package org.dromara.workflow.dubbo;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.workflow.api.RemoteWorkflowService;
import org.dromara.workflow.api.domain.RemoteCompleteTask;
import org.dromara.workflow.api.domain.RemoteStartProcess;
import org.dromara.workflow.api.domain.RemoteStartProcessReturn;
import org.dromara.workflow.api.domain.StartMessageEventDTO;
import org.dromara.workflow.api.domain.StartTimerEventDTO;
import org.dromara.workflow.api.domain.TriggerMessageEventDTO;
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

    @Override
    public boolean deleteInstance(List<String> businessIds) {
        return workflowService.deleteInstance(businessIds);
    }

    @Override
    public String getBusinessStatusByTaskId(Long taskId) {
        return workflowService.getBusinessStatusByTaskId(taskId);
    }

    @Override
    public String getBusinessStatus(String businessId) {
        return workflowService.getBusinessStatus(businessId);
    }

    @Override
    public void setVariable(Long instanceId, Map<String, Object> variable) {
        workflowService.setVariable(instanceId, variable);
    }

    @Override
    public Map<String, Object> instanceVariable(Long instanceId) {
        return workflowService.instanceVariable(instanceId);
    }

    @Override
    public Long getInstanceIdByBusinessId(String businessId) {
        return workflowService.getInstanceIdByBusinessId(businessId);
    }

    @Override
    public RemoteStartProcessReturn startWorkFlow(RemoteStartProcess startProcess) {
        return workflowService.startWorkFlow(startProcess);
    }

    @Override
    public boolean completeTask(RemoteCompleteTask completeTask) {
        return workflowService.completeTask(completeTask);
    }


    /**
     * 办理任务
     *
     * @param taskId  任务ID
     * @param message 办理意见
     * @return 结果
     */
    @Override
    public boolean completeTask(Long taskId, String message) {
        return workflowService.completeTask(taskId, message);
    }

    /**
     * 启动流程并办理第一个任务
     *
     * @param startProcess 参数
     * @return 结果
     */
    @Override
    public boolean startCompleteTask(RemoteStartProcess startProcess) {
        return workflowService.startCompleteTask(startProcess);
    }

    @Override
    public Long startByMessageEvent(StartMessageEventDTO request) {
        return workflowService.startByMessageEvent(request);
    }

    @Override
    public Long triggerMessageEvent(TriggerMessageEventDTO request) {
        return workflowService.triggerMessageEvent(request);
    }

    @Override
    public Long startByTimerEvent(StartTimerEventDTO request) {
        return workflowService.startByTimerEvent(request);
    }

    @Override
    public boolean throwInstanceErrorEvent(Long instanceId, String errorCode, String errorMessage) {
        return workflowService.throwInstanceErrorEvent(instanceId, errorCode, errorMessage);
    }

    @Override
    public boolean throwTaskErrorEvent(Long taskId, String errorCode, String errorMessage) {
        return workflowService.throwTaskErrorEvent(taskId, errorCode, errorMessage);
    }

}
