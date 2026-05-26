package com.ruoyi.system.crm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CRM 行为数据 Excel 导入参数（与 application.yml 中 crm.behavior.import 对应）。
 */
@ConfigurationProperties(prefix = "crm.behavior.import")
public class CrmBehaviorImportProperties
{
    /** EasyExcel 攒批 / JDBC executeBatch 行数 */
    private int batchSize = 2000;

    /** 并行落库消费者线程数（解析单线程，落库多线程） */
    private int consumerThreads = 4;

    /** 有界队列容量（批次数，用于解析与落库解耦） */
    private int queueBatchCapacity = 48;

    /** 每批 Excel 行转实体是否使用并行流（多核加速校验/日期解析） */
    private boolean parallelParse = true;

    /** 进度上报步长（行） */
    private int progressStep = 5000;

    /** Oracle JDBC 批量默认值（对应 oracle.jdbc.defaultBatchValue） */
    private int oracleDefaultBatchValue = 2000;

    public int getBatchSize()
    {
        return batchSize;
    }

    public void setBatchSize(int batchSize)
    {
        this.batchSize = batchSize;
    }

    public int getConsumerThreads()
    {
        return consumerThreads;
    }

    public void setConsumerThreads(int consumerThreads)
    {
        this.consumerThreads = consumerThreads;
    }

    public int getQueueBatchCapacity()
    {
        return queueBatchCapacity;
    }

    public void setQueueBatchCapacity(int queueBatchCapacity)
    {
        this.queueBatchCapacity = queueBatchCapacity;
    }

    public boolean isParallelParse()
    {
        return parallelParse;
    }

    public void setParallelParse(boolean parallelParse)
    {
        this.parallelParse = parallelParse;
    }

    public int getProgressStep()
    {
        return progressStep;
    }

    public void setProgressStep(int progressStep)
    {
        this.progressStep = progressStep;
    }

    public int getOracleDefaultBatchValue()
    {
        return oracleDefaultBatchValue;
    }

    public void setOracleDefaultBatchValue(int oracleDefaultBatchValue)
    {
        this.oracleDefaultBatchValue = oracleDefaultBatchValue;
    }
}
