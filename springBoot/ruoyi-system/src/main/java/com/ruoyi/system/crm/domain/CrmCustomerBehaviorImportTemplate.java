package com.ruoyi.system.crm.domain;

import com.ruoyi.common.annotation.Excel;

/**
 * 客户行为 Excel 导入模板行
 */
public class CrmCustomerBehaviorImportTemplate
{
    @Excel(name = "客户ID", prompt = "数字，如 1")
    private Long customerId;

    @Excel(name = "行为类型", prompt = "电话沟通/拜访/邮件/演示")
    private String behaviorType;

    @Excel(name = "描述")
    private String description;

    @Excel(name = "行为时间", dateFormat = "yyyy-MM-dd HH:mm:ss", prompt = "格式：yyyy-MM-dd HH:mm:ss")
    private String behaviorTime;

    public Long getCustomerId()
    {
        return customerId;
    }

    public void setCustomerId(Long customerId)
    {
        this.customerId = customerId;
    }

    public String getBehaviorType()
    {
        return behaviorType;
    }

    public void setBehaviorType(String behaviorType)
    {
        this.behaviorType = behaviorType;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getBehaviorTime()
    {
        return behaviorTime;
    }

    public void setBehaviorTime(String behaviorTime)
    {
        this.behaviorTime = behaviorTime;
    }
}
