package com.ruoyi.system.crm.support;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelAnalysisException;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.crm.domain.CrmCustomerBehavior;
import com.ruoyi.system.crm.domain.CrmCustomerBehaviorExcelRow;

/**
 * 基于 Alibaba EasyExcel 的行为数据导入解析（流式、低内存）
 */
public final class CrmBehaviorEasyExcelImporter
{
    private static final String[] DATE_PATTERNS = {
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd"
    };

    private static final ThreadLocal<SimpleDateFormat[]> DATE_FORMATTERS = ThreadLocal.withInitial(() -> {
        SimpleDateFormat[] formatters = new SimpleDateFormat[DATE_PATTERNS.length];
        for (int i = 0; i < DATE_PATTERNS.length; i++)
        {
            formatters[i] = new SimpleDateFormat(DATE_PATTERNS[i]);
        }
        return formatters;
    });

    private CrmBehaviorEasyExcelImporter()
    {
    }

    /** 统计有效数据行数（流式计数，可与导入并行执行） */
    public static int countValidRows(File file)
    {
        CrmBehaviorExcelCountListener listener = new CrmBehaviorExcelCountListener();
        readSheet(file, listener);
        return listener.getCount();
    }

    /**
     * 流式读取：每攒够 batchSize 行回调一次（可选并行行转换），由调用方异步入队落库。
     */
    public static void readInBatches(File file, int batchSize, boolean parallelParse,
        Consumer<List<CrmCustomerBehavior>> batchConsumer)
    {
        CrmBehaviorExcelBatchListener listener =
            new CrmBehaviorExcelBatchListener(batchSize, parallelParse, batchConsumer);
        readSheet(file, listener);
    }

    private static void readSheet(File file, AnalysisEventListener<CrmCustomerBehaviorExcelRow> listener)
    {
        try
        {
            EasyExcel.read(file, CrmCustomerBehaviorExcelRow.class, listener)
                .sheet(0)
                .headRowNumber(1)
                .doRead();
        }
        catch (ExcelAnalysisException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof ServiceException)
            {
                throw (ServiceException) cause;
            }
            throw new ServiceException("Excel 解析失败：" + (cause != null ? cause.getMessage() : e.getMessage()));
        }
    }

    public static boolean isBlankRow(CrmCustomerBehaviorExcelRow row)
    {
        if (row == null)
        {
            return true;
        }
        return StringUtils.isEmpty(trim(row.getCustomerId()))
            && StringUtils.isEmpty(trim(row.getBehaviorType()))
            && StringUtils.isEmpty(trim(row.getDescription()))
            && StringUtils.isEmpty(trim(row.getBehaviorTime()));
    }

    public static CrmCustomerBehavior toEntity(CrmCustomerBehaviorExcelRow row, int excelRowNum) throws ParseException
    {
        String customerIdStr = trim(row.getCustomerId());
        String behaviorType = trim(row.getBehaviorType());
        String description = trim(row.getDescription());
        String behaviorTimeStr = trim(row.getBehaviorTime());

        if (StringUtils.isEmpty(customerIdStr))
        {
            throw new ParseException("第 " + excelRowNum + " 行：客户ID不能为空", excelRowNum);
        }
        if (StringUtils.isEmpty(behaviorType))
        {
            throw new ParseException("第 " + excelRowNum + " 行：行为类型不能为空", excelRowNum);
        }

        CrmCustomerBehavior behavior = new CrmCustomerBehavior();
        try
        {
            behavior.setCustomerId(parseLongId(customerIdStr));
        }
        catch (NumberFormatException e)
        {
            throw new ParseException("第 " + excelRowNum + " 行：客户ID须为数字", excelRowNum);
        }
        behavior.setBehaviorType(behaviorType);
        behavior.setDescription(StringUtils.isNotEmpty(description) ? description : behaviorType + "记录-" + excelRowNum);
        behavior.setBehaviorTime(StringUtils.isNotEmpty(behaviorTimeStr) ? parseDate(behaviorTimeStr) : new Date());
        return behavior;
    }

    private static String trim(String value)
    {
        return value == null ? "" : value.trim();
    }

    private static long parseLongId(String value)
    {
        String v = value.trim();
        if (v.endsWith(".0"))
        {
            v = v.substring(0, v.length() - 2);
        }
        if (v.contains("."))
        {
            return (long) Double.parseDouble(v);
        }
        return Long.parseLong(v);
    }

    private static Date parseDate(String value) throws ParseException
    {
        SimpleDateFormat[] formatters = DATE_FORMATTERS.get();
        for (SimpleDateFormat formatter : formatters)
        {
            try
            {
                return formatter.parse(value);
            }
            catch (ParseException ignored)
            {
            }
        }
        throw new ParseException("行为时间格式不正确：" + value, 0);
    }
}
