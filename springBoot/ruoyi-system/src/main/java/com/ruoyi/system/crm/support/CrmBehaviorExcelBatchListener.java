package com.ruoyi.system.crm.support;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.crm.domain.CrmCustomerBehavior;
import com.ruoyi.system.crm.domain.CrmCustomerBehaviorExcelRow;

/**
 * EasyExcel 流式读取监听器：缓冲原始行，满批后并行转换为实体并回调（内存仅保留当前批）。
 */
public class CrmBehaviorExcelBatchListener extends AnalysisEventListener<CrmCustomerBehaviorExcelRow>
{
    private final int batchSize;
    private final boolean parallelParse;
    private final Consumer<List<CrmCustomerBehavior>> batchConsumer;
    private final List<CrmCustomerBehaviorExcelRow> rowBuffer;
    private final List<Integer> rowNumBuffer;

    public CrmBehaviorExcelBatchListener(int batchSize, boolean parallelParse,
        Consumer<List<CrmCustomerBehavior>> batchConsumer)
    {
        this.batchSize = batchSize;
        this.parallelParse = parallelParse;
        this.batchConsumer = batchConsumer;
        this.rowBuffer = new ArrayList<>(batchSize);
        this.rowNumBuffer = new ArrayList<>(batchSize);
    }

    @Override
    public void invoke(CrmCustomerBehaviorExcelRow data, AnalysisContext context)
    {
        if (CrmBehaviorEasyExcelImporter.isBlankRow(data))
        {
            return;
        }
        rowBuffer.add(data);
        rowNumBuffer.add(context.readRowHolder().getRowIndex() + 1);
        if (rowBuffer.size() >= batchSize)
        {
            flush();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context)
    {
        flush();
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception
    {
        int row = context.readRowHolder().getRowIndex() + 1;
        throw new ServiceException("第 " + row + " 行解析失败：" + exception.getMessage());
    }

    public void flush()
    {
        if (rowBuffer.isEmpty())
        {
            return;
        }
        List<CrmCustomerBehavior> entities = convertBatch();
        batchConsumer.accept(entities);
        rowBuffer.clear();
        rowNumBuffer.clear();
    }

    private List<CrmCustomerBehavior> convertBatch()
    {
        int size = rowBuffer.size();
        if (!parallelParse || size < 256)
        {
            List<CrmCustomerBehavior> entities = new ArrayList<>(size);
            for (int i = 0; i < size; i++)
            {
                entities.add(toEntity(rowBuffer.get(i), rowNumBuffer.get(i)));
            }
            return entities;
        }
        return IntStream.range(0, size)
            .parallel()
            .mapToObj(i -> toEntity(rowBuffer.get(i), rowNumBuffer.get(i)))
            .collect(Collectors.toList());
    }

    private static CrmCustomerBehavior toEntity(CrmCustomerBehaviorExcelRow row, int excelRowNum)
    {
        try
        {
            return CrmBehaviorEasyExcelImporter.toEntity(row, excelRowNum);
        }
        catch (ParseException e)
        {
            throw new ServiceException(e.getMessage());
        }
    }
}
