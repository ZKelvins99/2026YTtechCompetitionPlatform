package com.ruoyi.system.crm.service;

import com.ruoyi.system.crm.domain.CrmWorkflowActionRequest;
import com.ruoyi.system.crm.domain.CrmWorkflowInstance;
import com.ruoyi.system.crm.domain.CrmWorkflowStartRequest;

public interface ICrmWorkflowService
{
    CrmWorkflowInstance startContractApproval(CrmWorkflowStartRequest request, Long userId);

    CrmWorkflowInstance getInstanceDetail(Long instanceId, Long userId);

    String getBpmnXml(Long instanceId);

    CrmWorkflowInstance approve(CrmWorkflowActionRequest request, Long userId);

    CrmWorkflowInstance reject(CrmWorkflowActionRequest request, Long userId);

    CrmWorkflowInstance countersign(CrmWorkflowActionRequest request, Long userId);

    CrmWorkflowInstance transfer(CrmWorkflowActionRequest request, Long userId);

    CrmWorkflowInstance rollback(Long instanceId, Long targetNodeId, String opinion, Long userId);

    CrmWorkflowInstance terminate(CrmWorkflowActionRequest request, Long userId);
}
