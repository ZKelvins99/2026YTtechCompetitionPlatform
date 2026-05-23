package com.ruoyi.system.crm.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 用户画像统计 crm_user_profile
 */
public class CrmUserProfile
{
    private Long id;
    private Long userId;
    private Long customerCount;
    private Long opportunityCount;
    private BigDecimal winRate;
    private BigDecimal totalAmount;
    private String industryDistribution;
    private String regionDistribution;
    private String monthlyPerformance;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCustomerCount() { return customerCount; }
    public void setCustomerCount(Long customerCount) { this.customerCount = customerCount; }
    public Long getOpportunityCount() { return opportunityCount; }
    public void setOpportunityCount(Long opportunityCount) { this.opportunityCount = opportunityCount; }
    public BigDecimal getWinRate() { return winRate; }
    public void setWinRate(BigDecimal winRate) { this.winRate = winRate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getIndustryDistribution() { return industryDistribution; }
    public void setIndustryDistribution(String industryDistribution) { this.industryDistribution = industryDistribution; }
    public String getRegionDistribution() { return regionDistribution; }
    public void setRegionDistribution(String regionDistribution) { this.regionDistribution = regionDistribution; }
    public String getMonthlyPerformance() { return monthlyPerformance; }
    public void setMonthlyPerformance(String monthlyPerformance) { this.monthlyPerformance = monthlyPerformance; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
