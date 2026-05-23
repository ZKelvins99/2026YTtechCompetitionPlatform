package com.ruoyi.system.crm.domain;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商机对象 crm_opportunity
 */
public class CrmOpportunity extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "商机ID", cellType = ColumnType.NUMERIC)
    private Long id;

    @Excel(name = "商机名称")
    private String opportunityName;

    @Excel(name = "客户ID", cellType = ColumnType.NUMERIC)
    private Long customerId;

    @Excel(name = "客户名称")
    private String customerName;

    @Excel(name = "阶段ID", cellType = ColumnType.NUMERIC)
    private Long stageId;

    @Excel(name = "阶段名称")
    private String stageName;

    @Excel(name = "阶段编码")
    private String stageCode;

    @Excel(name = "预计金额", cellType = ColumnType.NUMERIC)
    private BigDecimal estimatedAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "预计成交日期", width = 20, dateFormat = "yyyy-MM-dd")
    private Date expectedCloseDate;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @NotBlank(message = "商机名称不能为空")
    @Size(max = 100, message = "商机名称不能超过100个字符")
    public String getOpportunityName()
    {
        return opportunityName;
    }

    public void setOpportunityName(String opportunityName)
    {
        this.opportunityName = opportunityName;
    }

    @NotNull(message = "关联客户不能为空")
    public Long getCustomerId()
    {
        return customerId;
    }

    public void setCustomerId(Long customerId)
    {
        this.customerId = customerId;
    }

    public String getCustomerName()
    {
        return customerName;
    }

    public void setCustomerName(String customerName)
    {
        this.customerName = customerName;
    }

    public Long getStageId()
    {
        return stageId;
    }

    public void setStageId(Long stageId)
    {
        this.stageId = stageId;
    }

    public String getStageName()
    {
        return stageName;
    }

    public void setStageName(String stageName)
    {
        this.stageName = stageName;
    }

    public String getStageCode()
    {
        return stageCode;
    }

    public void setStageCode(String stageCode)
    {
        this.stageCode = stageCode;
    }

    public BigDecimal getEstimatedAmount()
    {
        return estimatedAmount;
    }

    public void setEstimatedAmount(BigDecimal estimatedAmount)
    {
        this.estimatedAmount = estimatedAmount;
    }

    public Date getExpectedCloseDate()
    {
        return expectedCloseDate;
    }

    public void setExpectedCloseDate(Date expectedCloseDate)
    {
        this.expectedCloseDate = expectedCloseDate;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("opportunityName", getOpportunityName())
            .append("customerId", getCustomerId())
            .append("stageId", getStageId())
            .append("estimatedAmount", getEstimatedAmount())
            .toString();
    }
}
