package com.ruoyi.system.crm.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.crm.domain.CrmBehaviorTaskStatus;
import com.ruoyi.system.crm.domain.CrmCustomerBehavior;
import com.ruoyi.system.crm.mapper.CrmCustomerBehaviorMapper;
import com.ruoyi.system.crm.service.ICrmCustomerBehaviorService;
import com.ruoyi.system.crm.support.CrmBehaviorTaskManager;

@Service
public class CrmCustomerBehaviorServiceImpl implements ICrmCustomerBehaviorService
{
    @Autowired
    private CrmCustomerBehaviorMapper crmCustomerBehaviorMapper;

    @Autowired
    private CrmBehaviorTaskManager taskManager;

    @Autowired
    private CrmCustomerBehaviorAsyncService asyncService;

    @Override
    public String startGenerate(int count)
    {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        taskManager.createTask(taskId, count);
        asyncService.generateAsync(taskId, count);
        return taskId;
    }

    @Override
    public CrmBehaviorTaskStatus getTaskStatus(String taskId)
    {
        return taskManager.getTask(taskId);
    }

    @Override
    public Map<String, Object> scrollList(Long lastId, int pageSize)
    {
        if (lastId == null)
        {
            lastId = 0L;
        }
        if (pageSize <= 0 || pageSize > 500)
        {
            pageSize = 100;
        }
        List<CrmCustomerBehavior> list = crmCustomerBehaviorMapper.selectScrollList(lastId, pageSize);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        Long newLastId = lastId;
        if (!list.isEmpty())
        {
            newLastId = list.get(list.size() - 1).getId();
        }
        data.put("lastId", newLastId);
        data.put("hasMore", list.size() >= pageSize);
        data.put("total", crmCustomerBehaviorMapper.countTotal());
        return data;
    }

    @Override
    public long countTotal()
    {
        return crmCustomerBehaviorMapper.countTotal();
    }
}
