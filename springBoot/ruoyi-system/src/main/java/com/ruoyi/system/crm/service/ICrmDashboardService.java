package com.ruoyi.system.crm.service;

import java.util.Map;

/**
 * 数据大屏服务接口
 * <p>
 * 提供系统状态（CPU/内存/磁盘/JVM）实时采集和日志统计数据聚合，
 * 供前端大屏可视化展示。</p>
 */
public interface ICrmDashboardService
{
    /**
     * 获取系统实时状态（CPU 使用率、内存使用率、磁盘使用率、JVM 堆内存使用率）
     * @return map 包含 cpuUsage / memUsage / diskUsage / jvmHeapUsage
     */
    Map<String, Object> getSystemStatus() throws Exception;

    /**
     * 获取日志统计数据（今日操作频次、异常占比、TOP5 活跃用户、模块调用分布、最近日志列表）
     * @return map 包含 todayTotal / errorCount / hourlyStats / topUsers / moduleStats / recentLogs 等
     */
    Map<String, Object> getLogStats();
}
