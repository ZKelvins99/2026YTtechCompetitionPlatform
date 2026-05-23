package com.ruoyi.system.crm.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 工作流节点
 */
public class CrmWorkflowNode
{
    private Long id;
    private Long instanceId;
    private String nodeName;
    private Integer nodeOrder;
    private Long approverId;
    private String approverName;
    private String approvalType;
    /** 0待审批 1通过 2驳回 3转办 4回退 */
    private String status;
    private String opinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;

  /** BPMN 元素ID，供前端染色 */
    private String bpmnElementId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getInstanceId() { return instanceId; }
    public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public Integer getNodeOrder() { return nodeOrder; }
    public void setNodeOrder(Integer nodeOrder) { this.nodeOrder = nodeOrder; }
    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }
    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }
    public String getApprovalType() { return approvalType; }
    public void setApprovalType(String approvalType) { this.approvalType = approvalType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOpinion() { return opinion; }
    public void setOpinion(String opinion) { this.opinion = opinion; }
    public Date getApproveTime() { return approveTime; }
    public void setApproveTime(Date approveTime) { this.approveTime = approveTime; }
    public String getBpmnElementId() { return bpmnElementId; }
    public void setBpmnElementId(String bpmnElementId) { this.bpmnElementId = bpmnElementId; }
}
