package com.ruoyi.system.crm.domain;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 合同对象 crm_contract
 */
public class CrmContract extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "合同编号")
    private String contractNo;

    @NotBlank(message = "合同名称不能为空")
    @Size(max = 100)
    @Excel(name = "合同名称")
    private String contractName;

    private Long customerId;

    @Excel(name = "客户名称")
    private String customerName;

    private Long opportunityId;

    @NotNull(message = "合同金额不能为空")
    @Excel(name = "合同金额", cellType = ColumnType.NUMERIC)
    private BigDecimal contractAmount;

    private Long statusId;

    @Excel(name = "状态")
    private String statusName;

    private String statusCode;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date signDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    private String content;

    private Long workflowInstanceId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContractNo() { return contractNo; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }
    public String getContractName() { return contractName; }
    public void setContractName(String contractName) { this.contractName = contractName; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Long getOpportunityId() { return opportunityId; }
    public void setOpportunityId(Long opportunityId) { this.opportunityId = opportunityId; }
    public BigDecimal getContractAmount() { return contractAmount; }
    public void setContractAmount(BigDecimal contractAmount) { this.contractAmount = contractAmount; }
    public Long getStatusId() { return statusId; }
    public void setStatusId(Long statusId) { this.statusId = statusId; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }
    public Date getSignDate() { return signDate; }
    public void setSignDate(Date signDate) { this.signDate = signDate; }
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getWorkflowInstanceId() { return workflowInstanceId; }
    public void setWorkflowInstanceId(Long workflowInstanceId) { this.workflowInstanceId = workflowInstanceId; }
}
