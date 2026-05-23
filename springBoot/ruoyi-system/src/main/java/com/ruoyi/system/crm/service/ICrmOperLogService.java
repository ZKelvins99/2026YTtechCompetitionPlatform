package com.ruoyi.system.crm.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.crm.domain.CrmOperLog;

public interface ICrmOperLogService
{
    CrmOperLog selectCrmOperLogById(Long id);

    List<CrmOperLog> selectCrmOperLogList(CrmOperLog operLog);
}
