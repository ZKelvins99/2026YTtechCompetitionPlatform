package com.ruoyi.system.crm.mapper;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.crm.domain.CrmOperLog;

public interface CrmOperLogMapper
{
    CrmOperLog selectCrmOperLogById(Long id);

    List<CrmOperLog> selectCrmOperLogList(CrmOperLog operLog);

    int insertCrmOperLog(CrmOperLog operLog);

    int countToday();

    int countYesterday();

    int countTodayError();

    List<Map<String, Object>> selectHourlyStats();

    List<Map<String, Object>> selectTopUsers();

    List<Map<String, Object>> selectModuleStats();

    List<CrmOperLog> selectRecentLogs(int limit);
}
