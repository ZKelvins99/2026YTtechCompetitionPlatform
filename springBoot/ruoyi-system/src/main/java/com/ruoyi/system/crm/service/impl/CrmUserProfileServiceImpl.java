package com.ruoyi.system.crm.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
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
    private static final String[] RADAR_NAMES = { "客户数", "商机数", "合同额", "赢单率", "活跃度", "回款率" };

    /** 雷达满分对应的业务目标（用于归一化到 0–100，与量纲无关） */
    private static final double TARGET_CUSTOMER = 50;
    private static final double TARGET_OPPORTUNITY = 30;
    /** 合同额 100 万元视为雷达满分 */
    private static final double TARGET_CONTRACT_AMOUNT = 1_000_000;

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
        SysUser user = sysUserService.selectUserById(userId);
        return buildChartData(profile, user != null ? user.getUserName() : null);
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

        Map<String, Object> radarMetrics = new LinkedHashMap<>();
        radarMetrics.put("customerCount", customerCount);
        radarMetrics.put("opportunityCount", opportunityCount);
        radarMetrics.put("contractAmount", totalAmount);
        radarMetrics.put("winRate", round1(winRate));
        radarMetrics.put("activityScore", activityScore);
        radarMetrics.put("paymentRate", round1(paymentRate));

        CrmUserProfile profile = new CrmUserProfile();
        profile.setUserId(userId);
        profile.setCustomerCount((long) customerCount);
        profile.setOpportunityCount((long) opportunityCount);
        profile.setWinRate(BigDecimal.valueOf(winRate).setScale(2, RoundingMode.HALF_UP));
        profile.setTotalAmount(totalAmount);
        profile.setIndustryDistribution(JSON.toJSONString(industryStats));
        profile.setRegionDistribution(JSON.toJSONString(stageStats));
        profile.setMonthlyPerformance(JSON.toJSONString(radarMetrics));

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

    private Map<String, Object> buildChartData(CrmUserProfile profile, String createBy)
    {
        Map<String, Object> data = new HashMap<>();
        data.put("tagCloud", parseJsonArray(profile.getIndustryDistribution()));
        data.put("ringChart", parseJsonArray(profile.getRegionDistribution()));
        data.put("radarChart", buildRadarChart(profile, createBy));
        data.put("summary", Map.of(
            "customerCount", profile.getCustomerCount(),
            "opportunityCount", profile.getOpportunityCount(),
            "winRate", profile.getWinRate(),
            "totalAmount", profile.getTotalAmount()
        ));
        return data;
    }

    private Map<String, Object> buildRadarChart(CrmUserProfile profile, String createBy)
    {
        RadarMetrics metrics = resolveRadarMetrics(profile, createBy);

        List<Map<String, Object>> indicators = new ArrayList<>();
        List<Number> scores = new ArrayList<>();
        List<Map<String, Object>> rawMetrics = new ArrayList<>();

        addRadarDimension(indicators, scores, rawMetrics, 0, metrics.customerCount, TARGET_CUSTOMER, "家");
        addRadarDimension(indicators, scores, rawMetrics, 1, metrics.opportunityCount, TARGET_OPPORTUNITY, "个");
        addRadarDimension(indicators, scores, rawMetrics, 2, metrics.contractAmount, TARGET_CONTRACT_AMOUNT, "元");
        addRadarDimension(indicators, scores, rawMetrics, 3, metrics.winRate, 100, "%");
        addRadarDimension(indicators, scores, rawMetrics, 4, metrics.activityScore, 100, "分");
        addRadarDimension(indicators, scores, rawMetrics, 5, metrics.paymentRate, 100, "%");

        Map<String, Object> radarChart = new HashMap<>();
        radarChart.put("indicator", indicators);
        radarChart.put("data", List.of(Map.of("value", scores)));
        radarChart.put("rawMetrics", rawMetrics);
        return radarChart;
    }

    private void addRadarDimension(List<Map<String, Object>> indicators, List<Number> scores,
        List<Map<String, Object>> rawMetrics, int index, double rawValue, double targetFullScore, String unit)
    {
        indicators.add(Map.of("name", RADAR_NAMES[index], "max", 100));
        double score = normalizeToScore(rawValue, targetFullScore);
        scores.add(round1(score));
        Map<String, Object> raw = new HashMap<>();
        raw.put("name", RADAR_NAMES[index]);
        raw.put("value", formatRawValue(index, rawValue));
        raw.put("unit", unit);
        raw.put("score", round1(score));
        rawMetrics.add(raw);
    }

    private RadarMetrics resolveRadarMetrics(CrmUserProfile profile, String createBy)
    {
        RadarMetrics metrics = new RadarMetrics();
        metrics.customerCount = safeLong(profile.getCustomerCount());
        metrics.opportunityCount = safeLong(profile.getOpportunityCount());
        metrics.contractAmount = profile.getTotalAmount() != null
            ? profile.getTotalAmount().doubleValue() : 0;
        metrics.winRate = profile.getWinRate() != null ? profile.getWinRate().doubleValue() : 0;

        if (StringUtils.isNotEmpty(profile.getMonthlyPerformance()))
        {
            try
            {
                JSONObject json = JSON.parseObject(profile.getMonthlyPerformance());
                if (json != null && json.containsKey("customerCount"))
                {
                    metrics.customerCount = json.getDoubleValue("customerCount");
                    metrics.opportunityCount = json.getDoubleValue("opportunityCount");
                    Object amount = json.get("contractAmount");
                    if (amount instanceof BigDecimal)
                    {
                        metrics.contractAmount = ((BigDecimal) amount).doubleValue();
                    }
                    else if (amount instanceof Number)
                    {
                        metrics.contractAmount = ((Number) amount).doubleValue();
                    }
                    metrics.winRate = json.getDoubleValue("winRate");
                    metrics.activityScore = json.getDoubleValue("activityScore");
                    metrics.paymentRate = json.getDoubleValue("paymentRate");
                    return metrics;
                }
            }
            catch (Exception ignored)
            {
            }
            List<Integer> legacy = JSON.parseArray(profile.getMonthlyPerformance(), Integer.class);
            if (legacy != null && legacy.size() >= 6)
            {
                metrics.customerCount = legacy.get(0);
                metrics.opportunityCount = legacy.get(1);
                metrics.winRate = legacy.get(3);
                metrics.activityScore = legacy.get(4);
                metrics.paymentRate = legacy.get(5);
            }
        }

        if (StringUtils.isNotEmpty(createBy))
        {
            int operCount = crmUserProfileMapper.countRecentOperLogByOperator(createBy);
            metrics.activityScore = Math.min(100, operCount * 5);
            int contractTotal = crmUserProfileMapper.countContractByCreateBy(createBy);
            int activeContract = crmUserProfileMapper.countActiveContractByCreateBy(createBy);
            metrics.paymentRate = contractTotal > 0 ? (double) activeContract / contractTotal * 100 : 0;
        }
        return metrics;
    }

    private static double normalizeToScore(double value, double targetFullScore)
    {
        if (targetFullScore <= 0)
        {
            return 0;
        }
        return Math.min(100, Math.max(0, value / targetFullScore * 100));
    }

    private static Object formatRawValue(int index, double value)
    {
        if (index == 2)
        {
            return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
        }
        if (index >= 3)
        {
            return round1(value);
        }
        return (long) Math.round(value);
    }

    private static double safeLong(Long v)
    {
        return v != null ? v.doubleValue() : 0;
    }

    private static double round1(double v)
    {
        return Math.round(v * 10) / 10.0;
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

    private static class RadarMetrics
    {
        double customerCount;
        double opportunityCount;
        double contractAmount;
        double winRate;
        double activityScore;
        double paymentRate;
    }
}
