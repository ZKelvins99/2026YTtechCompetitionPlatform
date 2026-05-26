package com.ruoyi.system.crm.domain;

import com.alibaba.excel.annotation.ExcelProperty;

/**
 * 客户行为 Excel 导入行（EasyExcel 映射，列顺序与模板一致）
 */
public class CrmCustomerBehaviorExcelRow
{
    @ExcelProperty(index = 0)
    private String customerId;

    @ExcelProperty(index = 1)
    private String behaviorType;

    @ExcelProperty(index = 2)
    private String description;

    @ExcelProperty(index = 3)
    private String behaviorTime;

    public String getCustomerId()
    {
        return customerId;
    }

    public void setCustomerId(String customerId)
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
