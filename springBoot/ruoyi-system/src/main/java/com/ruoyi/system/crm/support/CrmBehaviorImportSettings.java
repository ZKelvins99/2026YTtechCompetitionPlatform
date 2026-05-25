package com.ruoyi.system.crm.support;

/**
 * 行为数据 Excel 导入管道参数。
 */
public final class CrmBehaviorImportSettings
{
    /** 并行落库消费者数（= crmBehaviorImportExecutor corePoolSize） */
    public static final int CONSUMER_THREADS = 4;

    /** 有界队列容量（批次数） */
    public static final int QUEUE_BATCH_CAPACITY = 32;

    /** 进度上报步长（行） */
    public static final int PROGRESS_STEP = 5000;

    /** JDBC executeBatch 每批行数 */
    public static final int BATCH_SIZE = 10000;

    private CrmBehaviorImportSettings()
    {
    }
}
