package com.ruoyi.system.crm.support;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import com.ruoyi.system.crm.domain.CrmBehaviorTaskStatus;

/**
 * 行为数据生成任务状态（内存存储）
 */
@Component
public class CrmBehaviorTaskManager
{
    private final ConcurrentHashMap<String, CrmBehaviorTaskStatus> tasks = new ConcurrentHashMap<>();

    public void createTask(String taskId, int total)
    {
        tasks.put(taskId, new CrmBehaviorTaskStatus(taskId, "RUNNING", 0, total));
    }

    public void updateProgress(String taskId, int processed)
    {
        CrmBehaviorTaskStatus task = tasks.get(taskId);
        if (task != null)
        {
            task.setProcessed(processed);
        }
    }

    public void markDone(String taskId)
    {
        CrmBehaviorTaskStatus task = tasks.get(taskId);
        if (task != null)
        {
            task.setStatus("DONE");
            task.setProcessed(task.getTotal());
        }
    }

    public void markFailed(String taskId, String message)
    {
        CrmBehaviorTaskStatus task = tasks.get(taskId);
        if (task != null)
        {
            task.setStatus("FAILED");
            task.setMessage(message);
        }
    }

    public CrmBehaviorTaskStatus getTask(String taskId)
    {
        return tasks.get(taskId);
    }
}
