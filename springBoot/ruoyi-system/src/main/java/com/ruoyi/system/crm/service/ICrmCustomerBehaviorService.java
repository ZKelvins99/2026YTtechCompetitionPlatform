package com.ruoyi.system.crm.service;

import java.util.Map;
import com.ruoyi.system.crm.domain.CrmBehaviorTaskStatus;

public interface ICrmCustomerBehaviorService
{
    String startGenerate(int count);

    CrmBehaviorTaskStatus getTaskStatus(String taskId);

    Map<String, Object> scrollList(Long lastId, int pageSize);

    long countTotal();
}
