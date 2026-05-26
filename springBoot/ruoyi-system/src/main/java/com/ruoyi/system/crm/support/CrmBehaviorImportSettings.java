package com.ruoyi.system.crm.support;

/**
 * 行为数据 Excel 导入管道常量（批次大小等以 {@link com.ruoyi.system.crm.config.CrmBehaviorImportProperties} 为准）。
 */
public final class CrmBehaviorImportSettings
{
    /** 默认批次行数（与 crm.behavior.import.batch-size 一致） */
    public static final int BATCH_SIZE = 2000;

    /** 进度上报步长（行） */
    public static final int PROGRESS_STEP = 5000;

    private CrmBehaviorImportSettings()
    {
    }
}
