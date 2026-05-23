package com.ruoyi.system.crm.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.crm.domain.CrmOperLog;
import com.ruoyi.system.crm.mapper.CrmOperLogMapper;
import com.ruoyi.system.crm.service.ICrmOperLogService;

@Service
public class CrmOperLogServiceImpl implements ICrmOperLogService
{
    @Autowired
    private CrmOperLogMapper crmOperLogMapper;

    @Override
    public CrmOperLog selectCrmOperLogById(Long id)
    {
        return crmOperLogMapper.selectCrmOperLogById(id);
    }

    @Override
    public List<CrmOperLog> selectCrmOperLogList(CrmOperLog operLog)
    {
        return crmOperLogMapper.selectCrmOperLogList(operLog);
    }
}
