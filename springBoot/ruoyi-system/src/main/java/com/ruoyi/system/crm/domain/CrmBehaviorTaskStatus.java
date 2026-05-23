package com.ruoyi.system.crm.domain;

/**
 * 行为数据异步生成任务状态
 */
public class CrmBehaviorTaskStatus
{
    private String taskId;
    /** RUNNING / DONE / FAILED */
    private String status;
    private int processed;
    private int total;
    private String message;

    public CrmBehaviorTaskStatus() { }

    public CrmBehaviorTaskStatus(String taskId, String status, int processed, int total)
    {
        this.taskId = taskId;
        this.status = status;
        this.processed = processed;
        this.total = total;
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
}
