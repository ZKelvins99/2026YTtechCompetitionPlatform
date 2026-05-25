package com.ruoyi.system.crm.domain;

import java.util.List;

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
    /** 部门环节会签人（可选，发起时选定） */
    private List<Long> deptCountersignIds;
    /** 财务环节会签人（可选） */
    private List<Long> financeCountersignIds;
    /** 归档环节会签人（可选） */
    private List<Long> archiveCountersignIds;

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public Long getDeptApproverId() { return deptApproverId; }
    public void setDeptApproverId(Long deptApproverId) { this.deptApproverId = deptApproverId; }
    public Long getFinanceApproverId() { return financeApproverId; }
    public void setFinanceApproverId(Long financeApproverId) { this.financeApproverId = financeApproverId; }
    public Long getArchiveApproverId() { return archiveApproverId; }
    public void setArchiveApproverId(Long archiveApproverId) { this.archiveApproverId = archiveApproverId; }
    public List<Long> getDeptCountersignIds() { return deptCountersignIds; }
    public void setDeptCountersignIds(List<Long> deptCountersignIds) { this.deptCountersignIds = deptCountersignIds; }
    public List<Long> getFinanceCountersignIds() { return financeCountersignIds; }
    public void setFinanceCountersignIds(List<Long> financeCountersignIds) { this.financeCountersignIds = financeCountersignIds; }
    public List<Long> getArchiveCountersignIds() { return archiveCountersignIds; }
    public void setArchiveCountersignIds(List<Long> archiveCountersignIds) { this.archiveCountersignIds = archiveCountersignIds; }
}
