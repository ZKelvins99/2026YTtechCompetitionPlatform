package com.ruoyi.system.crm.domain;

import java.util.List;

/**
 * 工作流操作请求
 */
public class CrmWorkflowActionRequest
{
    private Long instanceId;
    private String opinion;
    private Long approverId;
    private List<Long> countersignUserIds;

    public Long getInstanceId() { return instanceId; }
    public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
    public String getOpinion() { return opinion; }
    public void setOpinion(String opinion) { this.opinion = opinion; }
    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }
    public List<Long> getCountersignUserIds() { return countersignUserIds; }
    public void setCountersignUserIds(List<Long> countersignUserIds) { this.countersignUserIds = countersignUserIds; }
}
