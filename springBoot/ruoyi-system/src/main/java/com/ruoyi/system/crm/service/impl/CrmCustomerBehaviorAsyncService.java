package com.ruoyi.system.crm.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.ruoyi.system.crm.domain.CrmCustomerBehavior;
import com.ruoyi.system.crm.mapper.CrmCustomerBehaviorMapper;
import com.ruoyi.system.crm.support.CrmBehaviorTaskManager;

@Service
public class CrmCustomerBehaviorAsyncService
{
    private static final Logger log = LoggerFactory.getLogger(CrmCustomerBehaviorAsyncService.class);
    private static final int BATCH_SIZE = 500;
    private static final String[] BEHAVIOR_TYPES = { "电话沟通", "拜访", "邮件", "演示" };

    @Autowired
    private CrmCustomerBehaviorMapper crmCustomerBehaviorMapper;

    @Autowired
    private CrmBehaviorTaskManager taskManager;

    @Async("threadPoolTaskExecutor")
    public void generateAsync(String taskId, int count)
    {
        try
        {
            Random random = new Random();
            int processed = 0;
            while (processed < count)
            {
                int batchCount = Math.min(BATCH_SIZE, count - processed);
                List<CrmCustomerBehavior> batch = new ArrayList<>(batchCount);
                for (int i = 0; i < batchCount; i++)
                {
                    CrmCustomerBehavior row = new CrmCustomerBehavior();
                    row.setCustomerId((long) (random.nextInt(500) + 1));
                    String type = BEHAVIOR_TYPES[random.nextInt(BEHAVIOR_TYPES.length)];
                    row.setBehaviorType(type);
                    row.setDescription(type + "记录-" + (processed + i + 1));
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_YEAR, -random.nextInt(365));
                    row.setBehaviorTime(cal.getTime());
                    batch.add(row);
                }
                crmCustomerBehaviorMapper.batchInsert(batch);
                processed += batchCount;
                taskManager.updateProgress(taskId, processed);
            }
            taskManager.markDone(taskId);
        }
        catch (Exception e)
        {
            log.error("行为数据生成失败 taskId={}", taskId, e);
            taskManager.markFailed(taskId, e.getMessage());
        }
    }
}
