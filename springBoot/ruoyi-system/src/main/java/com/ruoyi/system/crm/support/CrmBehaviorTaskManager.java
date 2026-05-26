package com.ruoyi.system.crm.support;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.crm.domain.CrmBehaviorTaskStatus;

/**
 * 行为数据导入/生成任务状态（Redis 持久化，切页可恢复）
 */
@Component
public class CrmBehaviorTaskManager
{
    public static final String TASK_TYPE_IMPORT = "IMPORT";
    public static final String TASK_TYPE_GENERATE = "GENERATE";

    private static final String TASK_KEY_PREFIX = "crm:behavior:task:";
    private static final String ACTIVE_IMPORT_PREFIX = "crm:behavior:active:import:";
    private static final int TASK_TTL_HOURS = 24;

    private final ConcurrentHashMap<String, Object> taskLocks = new ConcurrentHashMap<>();

    @Autowired
    private RedisCache redisCache;

    public void assertNoRunningImport(Long userId)
    {
        if (userId == null)
        {
            return;
        }
        String taskId = redisCache.getCacheObject(activeImportKey(userId));
        if (StringUtils.isEmpty(taskId))
        {
            return;
        }
        CrmBehaviorTaskStatus task = getTask(taskId);
        if (task != null && "RUNNING".equals(task.getStatus()))
        {
            throw new ServiceException("已有 Excel 导入任务进行中，请等待完成后再导入");
        }
        redisCache.deleteObject(activeImportKey(userId));
    }

    public void createImportTask(String taskId, Long userId, int total, String fileName)
    {
        assertNoRunningImport(userId);
        CrmBehaviorTaskStatus status = new CrmBehaviorTaskStatus(taskId, "RUNNING", 0, total);
        status.setUserId(userId);
        status.setTaskType(TASK_TYPE_IMPORT);
        status.setFileName(fileName);
        saveTask(status);
        if (userId != null)
        {
            redisCache.setCacheObject(activeImportKey(userId), taskId, TASK_TTL_HOURS, TimeUnit.HOURS);
        }
    }

    public void createGenerateTask(String taskId, Long userId, int total)
    {
        CrmBehaviorTaskStatus status = new CrmBehaviorTaskStatus(taskId, "RUNNING", 0, total);
        status.setUserId(userId);
        status.setTaskType(TASK_TYPE_GENERATE);
        saveTask(status);
    }

    public void updateProgress(String taskId, int processed)
    {
        synchronized (lockFor(taskId))
        {
            CrmBehaviorTaskStatus status = loadTask(taskId);
            if (status == null)
            {
                return;
            }
            status.setProcessed(processed);
            refreshMetrics(status);
            saveTask(status);
        }
    }

    public void updateTotal(String taskId, int total)
    {
        synchronized (lockFor(taskId))
        {
            CrmBehaviorTaskStatus status = loadTask(taskId);
            if (status == null)
            {
                return;
            }
            status.setTotal(total);
            saveTask(status);
        }
    }

    public void markDone(String taskId)
    {
        synchronized (lockFor(taskId))
        {
            CrmBehaviorTaskStatus status = loadTask(taskId);
            if (status == null)
            {
                return;
            }
            int p = status.getProcessed();
            if (TASK_TYPE_IMPORT.equals(status.getTaskType()) && p <= 0)
            {
                status.setStatus("FAILED");
                status.setMessage("未导入任何数据");
                status.setProcessed(0);
                refreshMetrics(status);
                saveTask(status);
                clearActiveImportIfMatch(status.getUserId(), taskId);
                return;
            }
            if (status.getTotal() <= 0 || p < status.getTotal())
            {
                status.setTotal(p);
            }
            status.setProcessed(p);
            status.setStatus("DONE");
            refreshMetrics(status);
            saveTask(status);
            clearActiveImportIfMatch(status.getUserId(), taskId);
        }
    }

    public void markFailed(String taskId, String message)
    {
        synchronized (lockFor(taskId))
        {
            CrmBehaviorTaskStatus status = loadTask(taskId);
            if (status == null)
            {
                return;
            }
            status.setStatus("FAILED");
            status.setMessage(message);
            refreshMetrics(status);
            saveTask(status);
            clearActiveImportIfMatch(status.getUserId(), taskId);
        }
    }

    public CrmBehaviorTaskStatus getTask(String taskId)
    {
        CrmBehaviorTaskStatus status = loadTask(taskId);
        if (status == null)
        {
            return null;
        }
        if ("RUNNING".equals(status.getStatus()))
        {
            synchronized (lockFor(taskId))
            {
                status = loadTask(taskId);
                if (status != null)
                {
                    refreshMetrics(status);
                    saveTask(status);
                }
            }
        }
        return status;
    }

    /**
     * 仅返回进行中的导入任务；已完成/失败任务会清理 active 标记并返回 null。
     */
    public CrmBehaviorTaskStatus getActiveImportTask(Long userId)
    {
        if (userId == null)
        {
            return null;
        }
        String taskId = redisCache.getCacheObject(activeImportKey(userId));
        if (StringUtils.isEmpty(taskId))
        {
            return null;
        }
        CrmBehaviorTaskStatus task = loadTask(taskId);
        if (task == null)
        {
            redisCache.deleteObject(activeImportKey(userId));
            return null;
        }
        if (!"RUNNING".equals(task.getStatus()))
        {
            clearActiveImportIfMatch(userId, taskId);
            return null;
        }
        refreshMetrics(task);
        saveTask(task);
        return task;
    }

    private void clearActiveImportIfMatch(Long userId, String taskId)
    {
        if (userId == null || StringUtils.isEmpty(taskId))
        {
            return;
        }
        String active = redisCache.getCacheObject(activeImportKey(userId));
        if (taskId.equals(active))
        {
            redisCache.deleteObject(activeImportKey(userId));
        }
    }

    private void saveTask(CrmBehaviorTaskStatus status)
    {
        redisCache.setCacheObject(taskKey(status.getTaskId()), status, TASK_TTL_HOURS, TimeUnit.HOURS);
    }

    private CrmBehaviorTaskStatus loadTask(String taskId)
    {
        if (StringUtils.isEmpty(taskId))
        {
            return null;
        }
        return redisCache.getCacheObject(taskKey(taskId));
    }

    private void refreshMetrics(CrmBehaviorTaskStatus status)
    {
        long elapsed = Math.max(0, System.currentTimeMillis() - status.getStartTime());
        status.setElapsedMs(elapsed);
        if (elapsed > 0 && status.getProcessed() > 0)
        {
            status.setSpeedPerSec((int) Math.min(Integer.MAX_VALUE, status.getProcessed() * 1000L / elapsed));
        }
        else
        {
            status.setSpeedPerSec(0);
        }
    }

    private Object lockFor(String taskId)
    {
        return taskLocks.computeIfAbsent(taskId, k -> new Object());
    }

    private static String taskKey(String taskId)
    {
        return TASK_KEY_PREFIX + taskId;
    }

    private static String activeImportKey(Long userId)
    {
        return ACTIVE_IMPORT_PREFIX + userId;
    }
}
