package com.ruoyi.system.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.crm.domain.CrmWorkflowActionRequest;
import com.ruoyi.system.crm.service.ICrmWorkflowService;

@RestController
@RequestMapping("/crm/workflow")
public class CrmWorkflowController extends BaseController
{
    @Autowired
    private ICrmWorkflowService workflowService;

    @PreAuthorize("@ss.hasPermi('crm:workflow:start')")
    @Log(title = "合同审批", businessType = BusinessType.INSERT)
    @PostMapping("/start")
    public AjaxResult start(@RequestBody java.util.Map<String, Long> body)
    {
        Long contractId = body.get("contractId");
        return success(workflowService.startContractApproval(contractId, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('crm:workflow:query')")
    @GetMapping("/instance/{id}")
    public AjaxResult getInstance(@PathVariable Long id)
    {
        return success(workflowService.getInstanceDetail(id));
    }

    @PreAuthorize("@ss.hasPermi('crm:workflow:query')")
    @GetMapping("/instance/{id}/bpmn")
    public AjaxResult getBpmn(@PathVariable Long id)
    {
        return success(workflowService.getBpmnXml(id));
    }

    @PreAuthorize("@ss.hasPermi('crm:workflow:approve')")
    @Log(title = "合同审批", businessType = BusinessType.UPDATE)
    @PutMapping("/approve")
    public AjaxResult approve(@RequestBody CrmWorkflowActionRequest request)
    {
        return success(workflowService.approve(request, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('crm:workflow:approve')")
    @Log(title = "合同审批", businessType = BusinessType.UPDATE)
    @PutMapping("/reject")
    public AjaxResult reject(@RequestBody CrmWorkflowActionRequest request)
    {
        return success(workflowService.reject(request, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('crm:workflow:approve')")
    @Log(title = "合同审批", businessType = BusinessType.UPDATE)
    @PutMapping("/countersign")
    public AjaxResult countersign(@RequestBody CrmWorkflowActionRequest request)
    {
        return success(workflowService.countersign(request, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('crm:workflow:approve')")
    @Log(title = "合同审批", businessType = BusinessType.UPDATE)
    @PutMapping("/transfer")
    public AjaxResult transfer(@RequestBody CrmWorkflowActionRequest request)
    {
        return success(workflowService.transfer(request, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('crm:workflow:approve')")
    @Log(title = "合同审批", businessType = BusinessType.UPDATE)
    @PutMapping("/rollback/{targetNodeId}")
    public AjaxResult rollback(@PathVariable Long targetNodeId, @RequestBody CrmWorkflowActionRequest request)
    {
        return success(workflowService.rollback(request.getInstanceId(), targetNodeId,
            request.getOpinion(), getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('crm:workflow:approve')")
    @Log(title = "合同审批", businessType = BusinessType.UPDATE)
    @PutMapping("/terminate")
    public AjaxResult terminate(@RequestBody CrmWorkflowActionRequest request)
    {
        return success(workflowService.terminate(request, getUserId()));
    }
}
