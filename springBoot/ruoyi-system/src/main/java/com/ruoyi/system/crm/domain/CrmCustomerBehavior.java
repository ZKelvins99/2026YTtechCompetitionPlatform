package com.ruoyi.system.crm.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 客户行为记录 crm_customer_behavior
 */
public class CrmCustomerBehavior
{
    private Long id;
    private Long customerId;
    private String behaviorType;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date behaviorTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getBehaviorType() { return behaviorType; }
    public void setBehaviorType(String behaviorType) { this.behaviorType = behaviorType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Date getBehaviorTime() { return behaviorTime; }
    public void setBehaviorTime(Date behaviorTime) { this.behaviorTime = behaviorTime; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
