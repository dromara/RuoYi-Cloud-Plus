package org.dromara.workflow.dubbo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.workflow.api.domain.RemoteWorkflowService;
import org.dromara.workflow.api.domain.dto.BusinessInstanceDTO;
import org.dromara.workflow.common.enums.BusinessStatusEnum;
import org.dromara.workflow.domain.ActHiProcinst;
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
    public BusinessInstanceDTO getBusinessInstance(String businessKey) {

        ActHiProcinst actHiProcinst = actHiProcinstService.selectByBusinessKey(businessKey);
        if (actHiProcinst == null) {
            BusinessInstanceDTO businessInstanceDTO = new BusinessInstanceDTO();
            businessInstanceDTO.setBusinessStatus(BusinessStatusEnum.DRAFT.getStatus());
            return businessInstanceDTO;
        }

        BusinessInstanceDTO businessInstanceDTO = BeanUtil.toBean(actHiProcinst, BusinessInstanceDTO.class);
        businessInstanceDTO.setBusinessStatusName(BusinessStatusEnum.findByStatus(businessInstanceDTO.getBusinessStatus()));
        businessInstanceDTO.setProcessDefinitionId(actHiProcinst.getProcDefId());
        return businessInstanceDTO;
    }

    @Override
    public List<BusinessInstanceDTO> getBusinessInstance(List<String> businessKeys) {
        List<ActHiProcinst> actHiProcinstList = actHiProcinstService.selectByBusinessKeyIn(businessKeys);
        List<BusinessInstanceDTO> businessInstanceList = BeanUtil.copyToList(actHiProcinstList, BusinessInstanceDTO.class,
            CopyOptions.create().setFieldMapping(Map.of("procDefId", "processDefinitionId")));
        businessInstanceList.forEach(dto -> {
            dto.setBusinessStatusName(BusinessStatusEnum.findByStatus(dto.getBusinessStatus()));
        });

        return businessInstanceList;
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
}
