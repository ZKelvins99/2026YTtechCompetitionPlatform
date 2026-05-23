package com.ruoyi.system.crm.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.crm.domain.CrmUserProfile;
import com.ruoyi.system.crm.mapper.CrmUserProfileMapper;
import com.ruoyi.system.crm.service.ICrmUserProfileService;
import com.ruoyi.system.service.ISysUserService;

@Service
public class CrmUserProfileServiceImpl implements ICrmUserProfileService
{
    @Autowired
    private CrmUserProfileMapper crmUserProfileMapper;

    @Autowired
    private ISysUserService sysUserService;

    @Override
    public Map<String, Object> getProfile(Long userId)
    {
        refresh(userId);
        CrmUserProfile profile = crmUserProfileMapper.selectByUserId(userId);
        if (profile == null)
        {
            throw new ServiceException("用户画像数据不存在");
        }
        return buildChartData(profile);
    }

    @Override
    public void refresh(Long userId)
    {
        SysUser user = sysUserService.selectUserById(userId);
        if (user == null)
        {
            throw new ServiceException("用户不存在");
        }
        String createBy = user.getUserName();

        int customerCount = crmUserProfileMapper.countCustomerByCreateBy(createBy);
        int opportunityCount = crmUserProfileMapper.countOpportunityByCreateBy(createBy);
        int winCount = crmUserProfileMapper.countWinOpportunityByCreateBy(createBy);
        BigDecimal totalAmount = crmUserProfileMapper.sumContractAmountByCreateBy(createBy);
        if (totalAmount == null)
        {
            totalAmount = BigDecimal.ZERO;
        }

        double winRate = opportunityCount > 0 ? (double) winCount / opportunityCount * 100 : 0;
        int contractTotal = crmUserProfileMapper.countContractByCreateBy(createBy);
        int activeContract = crmUserProfileMapper.countActiveContractByCreateBy(createBy);
        double paymentRate = contractTotal > 0 ? (double) activeContract / contractTotal * 100 : 0;

        int operCount = crmUserProfileMapper.countRecentOperLogByOperator(createBy);
        int activityScore = Math.min(100, operCount * 5);

        List<Map<String, Object>> industryStats = normalizeStats(crmUserProfileMapper.selectIndustryStats(createBy));
        List<Map<String, Object>> stageStats = normalizeStats(crmUserProfileMapper.selectStageStats(createBy));

        List<Integer> radarValues = List.of(
            customerCount,
            opportunityCount,
            totalAmount.setScale(0, RoundingMode.HALF_UP).intValue(),
            (int) Math.round(winRate),
            activityScore,
            (int) Math.round(paymentRate)
        );

        CrmUserProfile profile = new CrmUserProfile();
        profile.setUserId(userId);
        profile.setCustomerCount((long) customerCount);
        profile.setOpportunityCount((long) opportunityCount);
        profile.setWinRate(BigDecimal.valueOf(winRate).setScale(2, RoundingMode.HALF_UP));
        profile.setTotalAmount(totalAmount);
        profile.setIndustryDistribution(JSON.toJSONString(industryStats));
        profile.setRegionDistribution(JSON.toJSONString(stageStats));
        profile.setMonthlyPerformance(JSON.toJSONString(radarValues));

        CrmUserProfile existing = crmUserProfileMapper.selectByUserId(userId);
        if (existing == null)
        {
            crmUserProfileMapper.insertCrmUserProfile(profile);
        }
        else
        {
            crmUserProfileMapper.updateCrmUserProfile(profile);
        }
    }

    @Override
    @Async("threadPoolTaskExecutor")
    public void refreshAsync(Long userId)
    {
        if (userId == null)
        {
            return;
        }
        try
        {
            refresh(userId);
        }
        catch (Exception ignored)
        {
        }
    }

    private Map<String, Object> buildChartData(CrmUserProfile profile)
    {
        Map<String, Object> data = new HashMap<>();
        data.put("tagCloud", parseJsonArray(profile.getIndustryDistribution()));
        data.put("ringChart", parseJsonArray(profile.getRegionDistribution()));

        List<Integer> values = JSON.parseArray(profile.getMonthlyPerformance(), Integer.class);
        if (values == null || values.size() < 6)
        {
            values = List.of(0, 0, 0, 0, 0, 0);
        }
        Map<String, Object> radarChart = new HashMap<>();
        radarChart.put("indicator", List.of(
            Map.of("name", "客户数", "max", 100),
            Map.of("name", "商机数", "max", 50),
            Map.of("name", "合同额", "max", 1000),
            Map.of("name", "赢单率", "max", 100),
            Map.of("name", "活跃度", "max", 100),
            Map.of("name", "回款率", "max", 100)
        ));
        radarChart.put("data", List.of(Map.of("value", values)));
        data.put("radarChart", radarChart);
        data.put("summary", Map.of(
            "customerCount", profile.getCustomerCount(),
            "opportunityCount", profile.getOpportunityCount(),
            "winRate", profile.getWinRate(),
            "totalAmount", profile.getTotalAmount()
        ));
        return data;
    }

    private List<Map<String, Object>> parseJsonArray(String json)
    {
        if (StringUtils.isEmpty(json))
        {
            return new ArrayList<>();
        }
        List<Map> raw = JSON.parseArray(json, Map.class);
        List<Map<String, Object>> result = new ArrayList<>();
        if (raw != null)
        {
            for (Map item : raw)
            {
                Map<String, Object> row = new HashMap<>();
                Object name = item.get("name");
                if (name == null) name = item.get("NAME");
                Object value = item.get("value");
                if (value == null) value = item.get("VALUE");
                row.put("name", String.valueOf(name));
                row.put("value", value instanceof Number ? ((Number) value).intValue() : 0);
                result.add(row);
            }
        }
        return result;
    }

    private List<Map<String, Object>> normalizeStats(List<Map<String, Object>> dbStats)
    {
        List<Map<String, Object>> result = new ArrayList<>();
        if (dbStats == null)
        {
            return result;
        }
        for (Map<String, Object> row : dbStats)
        {
            Map<String, Object> item = new HashMap<>();
            Object name = row.get("name");
            if (name == null) name = row.get("NAME");
            Object value = row.get("value");
            if (value == null) value = row.get("VALUE");
            item.put("name", name);
            item.put("value", value instanceof Number ? ((Number) value).intValue() : 0);
            result.add(item);
        }
        return result;
    }
}
