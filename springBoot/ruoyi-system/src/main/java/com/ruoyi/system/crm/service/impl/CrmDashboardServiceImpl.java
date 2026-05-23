package com.ruoyi.system.crm.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.Arith;
import com.ruoyi.system.crm.domain.CrmOperLog;
import com.ruoyi.system.crm.mapper.CrmOperLogMapper;
import com.ruoyi.system.crm.service.ICrmDashboardService;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

@Service
public class CrmDashboardServiceImpl implements ICrmDashboardService
{
  private static final int OSHI_WAIT = 500;

    @Autowired
    private CrmOperLogMapper crmOperLogMapper;

    @Override
    public Map<String, Object> getSystemStatus() throws Exception
    {
        Map<String, Object> data = new HashMap<>();
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        CentralProcessor processor = hal.getProcessor();

        long[] prevTicks = processor.getSystemCpuLoadTicks();
        Thread.sleep(OSHI_WAIT);
        double cpuUsage = Arith.round(processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100, 1);

        GlobalMemory memory = hal.getMemory();
        double memUsage = Arith.round((1.0 - (double) memory.getAvailable() / memory.getTotal()) * 100, 1);

        OperatingSystem os = si.getOperatingSystem();
        FileSystem fs = os.getFileSystem();
        long total = 0, used = 0;
        for (OSFileStore store : fs.getFileStores())
        {
            total += store.getTotalSpace();
            used += store.getTotalSpace() - store.getUsableSpace();
        }
        double diskUsage = total > 0 ? Arith.round((double) used / total * 100, 1) : 0;

        Runtime runtime = Runtime.getRuntime();
        long jvmTotal = runtime.totalMemory();
        long jvmFree = runtime.freeMemory();
        double jvmHeapUsage = Arith.round((double) (jvmTotal - jvmFree) / jvmTotal * 100, 1);

        data.put("cpuUsage", cpuUsage);
        data.put("memUsage", memUsage);
        data.put("diskUsage", diskUsage);
        data.put("jvmHeapUsage", jvmHeapUsage);
        return data;
    }

    @Override
    public Map<String, Object> getLogStats()
    {
        Map<String, Object> data = new HashMap<>();
        int today = crmOperLogMapper.countToday();
        int yesterday = crmOperLogMapper.countYesterday();
        int errorCount = crmOperLogMapper.countTodayError();

        double trend = 0;
        if (yesterday > 0)
        {
            trend = Arith.round((double) (today - yesterday) / yesterday * 100, 1);
        }
        double errorRate = today > 0 ? Arith.round((double) errorCount / today * 100, 1) : 0;

        data.put("todayTotal", today);
        data.put("yesterdayTotal", yesterday);
        data.put("todayTrend", trend);
        data.put("errorCount", errorCount);
        data.put("errorRate", errorRate);
        data.put("hourlyStats", fillHourly(crmOperLogMapper.selectHourlyStats()));
        data.put("topUsers", crmOperLogMapper.selectTopUsers());
        data.put("moduleStats", crmOperLogMapper.selectModuleStats());
        data.put("recentLogs", crmOperLogMapper.selectRecentLogs(20));
        return data;
    }

    private List<Map<String, Object>> fillHourly(List<Map<String, Object>> dbStats)
    {
        Map<String, Integer> map = new HashMap<>();
        if (dbStats != null)
        {
            for (Map<String, Object> row : dbStats)
            {
                Object hour = row.get("hour");
                if (hour == null) hour = row.get("HOUR");
                Object count = row.get("count");
                if (count == null) count = row.get("CNT");
                map.put(String.valueOf(hour), ((Number) count).intValue());
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int h = 0; h < 24; h++)
        {
            String key = String.format("%02d", h);
            Map<String, Object> item = new HashMap<>();
            item.put("hour", key);
            item.put("count", map.getOrDefault(key, 0));
            result.add(item);
        }
        return result;
    }
}
