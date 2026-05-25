package com.ruoyi.system.crm.domain;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工作流实例
 */
public class CrmWorkflowInstance extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String processType;
    private Long businessId;
    private String businessNo;
    private Long currentNodeId;
    private String status;
    private Long startUserId;
    private String startUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    private List<CrmWorkflowNode> nodes;

    /** 当前用户是否可操作当前节点（含管理员） */
    private Boolean canOperate;

    /** 当前活动环节序号（2=部门 3=财务 4=归档） */
    private Integer activeNodeOrder;

    /** 当前环节名称 */
    private String currentStageName;

    /** 当前待办人摘要（含会签） */
    private String pendingHint;

    /** BPMN 流程图 XML（与实例一并返回，避免单独接口丢数据） */
    private String bpmnXml;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProcessType() { return processType; }
    public void setProcessType(String processType) { this.processType = processType; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public String getBusinessNo() { return businessNo; }
    public void setBusinessNo(String businessNo) { this.businessNo = businessNo; }
    public Long getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(Long currentNodeId) { this.currentNodeId = currentNodeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getStartUserId() { return startUserId; }
    public void setStartUserId(Long startUserId) { this.startUserId = startUserId; }
    public String getStartUserName() { return startUserName; }
    public void setStartUserName(String startUserName) { this.startUserName = startUserName; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public List<CrmWorkflowNode> getNodes() { return nodes; }
    public void setNodes(List<CrmWorkflowNode> nodes) { this.nodes = nodes; }
    public Boolean getCanOperate() { return canOperate; }
    public void setCanOperate(Boolean canOperate) { this.canOperate = canOperate; }
    public Integer getActiveNodeOrder() { return activeNodeOrder; }
    public void setActiveNodeOrder(Integer activeNodeOrder) { this.activeNodeOrder = activeNodeOrder; }
    public String getCurrentStageName() { return currentStageName; }
    public void setCurrentStageName(String currentStageName) { this.currentStageName = currentStageName; }
    public String getPendingHint() { return pendingHint; }
    public void setPendingHint(String pendingHint) { this.pendingHint = pendingHint; }
    public String getBpmnXml() { return bpmnXml; }
    public void setBpmnXml(String bpmnXml) { this.bpmnXml = bpmnXml; }
}
