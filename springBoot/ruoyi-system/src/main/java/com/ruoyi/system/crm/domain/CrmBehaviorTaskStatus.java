package com.ruoyi.system.crm.domain;

/**
 * 行为数据异步任务状态
 */
public class CrmBehaviorTaskStatus
{
    private String taskId;
    /** RUNNING / DONE / FAILED */
    private String status;
    private int processed;
    private int total;
    private String message;
    /** 任务开始时间戳（毫秒） */
    private long startTime;
    /** 已耗时（毫秒） */
    private long elapsedMs;
    /** 导入速度（条/秒） */
    private int speedPerSec;
    /** 发起人用户 ID */
    private Long userId;
    /** IMPORT / GENERATE */
    private String taskType;
    /** 导入文件名 */
    private String fileName;

    public CrmBehaviorTaskStatus() { }

    public CrmBehaviorTaskStatus(String taskId, String status, int processed, int total)
    {
        this.taskId = taskId;
        this.status = status;
        this.processed = processed;
        this.total = total;
        this.startTime = System.currentTimeMillis();
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getProcessed() { return processed; }
    public void setProcessed(int processed) { this.processed = processed; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }
    public int getSpeedPerSec() { return speedPerSec; }
    public void setSpeedPerSec(int speedPerSec) { this.speedPerSec = speedPerSec; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}
