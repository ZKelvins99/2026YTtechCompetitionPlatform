package com.ruoyi.system.crm.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 行为数据批量生成请求
 */
public class CrmBehaviorGenerateRequest
{
    @NotNull(message = "生成数量不能为空")
    @Min(value = 1, message = "生成数量至少为1")
    @Max(value = 500000, message = "单次生成数量不能超过500000")
    private Integer count;

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
}
