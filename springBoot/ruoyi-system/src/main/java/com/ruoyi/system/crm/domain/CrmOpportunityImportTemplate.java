package com.ruoyi.system.crm.domain;

import com.ruoyi.common.annotation.Excel;

/**
 * 商机 Excel 导入模板行
 */
public class CrmOpportunityImportTemplate
{
    @Excel(name = "商机名称")
    private String opportunityName;

    @Excel(name = "客户名称")
    private String customerName;

    @Excel(name = "阶段编码", prompt = "CONTACT/ANALYSIS/QUOTE/NEGOTIATE/WIN/LOST")
    private String stageCode;

    @Excel(name = "预计金额")
    private String estimatedAmount;

    @Excel(name = "预计成交日期", dateFormat = "yyyy-MM-dd", prompt = "格式：yyyy-MM-dd")
    private String expectedCloseDate;

    public String getOpportunityName()
    {
        return opportunityName;
    }

    public void setOpportunityName(String opportunityName)
    {
        this.opportunityName = opportunityName;
    }

    public String getCustomerName()
    {
        return customerName;
    }

    public void setCustomerName(String customerName)
    {
        this.customerName = customerName;
    }

    public String getStageCode()
    {
        return stageCode;
    }

    public void setStageCode(String stageCode)
    {
        this.stageCode = stageCode;
    }

    public String getEstimatedAmount()
    {
        return estimatedAmount;
    }

    public void setEstimatedAmount(String estimatedAmount)
    {
        this.estimatedAmount = estimatedAmount;
    }

    public String getExpectedCloseDate()
    {
        return expectedCloseDate;
    }

    public void setExpectedCloseDate(String expectedCloseDate)
    {
        this.expectedCloseDate = expectedCloseDate;
    }
}
