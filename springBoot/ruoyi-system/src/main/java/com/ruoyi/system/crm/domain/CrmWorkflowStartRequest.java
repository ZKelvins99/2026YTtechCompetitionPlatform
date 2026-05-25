package com.ruoyi.system.crm.domain;

/**
 * 发起合同审批
 */
public class CrmWorkflowStartRequest
{
    private Long contractId;
    /** 部门审批人 */
    private Long deptApproverId;
    /** 财务审批人 */
    private Long financeApproverId;
    /** 归档审批人 */
    private Long archiveApproverId;

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public Long getDeptApproverId() { return deptApproverId; }
    public void setDeptApproverId(Long deptApproverId) { this.deptApproverId = deptApproverId; }
    public Long getFinanceApproverId() { return financeApproverId; }
    public void setFinanceApproverId(Long financeApproverId) { this.financeApproverId = financeApproverId; }
    public Long getArchiveApproverId() { return archiveApproverId; }
    public void setArchiveApproverId(Long archiveApproverId) { this.archiveApproverId = archiveApproverId; }
}
