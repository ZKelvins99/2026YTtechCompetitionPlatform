package com.ruoyi.system.crm.support;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.ruoyi.system.crm.domain.CrmCustomerBehaviorExcelRow;

/**
 * EasyExcel 流式统计有效行数（仅计数，不缓存全部行）。
 */
public class CrmBehaviorExcelCountListener extends AnalysisEventListener<CrmCustomerBehaviorExcelRow>
{
    private int count;

    @Override
    public void invoke(CrmCustomerBehaviorExcelRow data, AnalysisContext context)
    {
        if (!CrmBehaviorEasyExcelImporter.isBlankRow(data))
        {
            count++;
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context)
    {
    }

    public int getCount()
    {
        return count;
    }
}
