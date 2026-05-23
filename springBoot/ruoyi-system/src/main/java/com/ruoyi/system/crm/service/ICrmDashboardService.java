package com.ruoyi.system.crm.service;

import java.util.Map;

public interface ICrmDashboardService
{
    Map<String, Object> getSystemStatus() throws Exception;

    Map<String, Object> getLogStats();
}
