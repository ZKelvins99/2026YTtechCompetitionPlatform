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
}
