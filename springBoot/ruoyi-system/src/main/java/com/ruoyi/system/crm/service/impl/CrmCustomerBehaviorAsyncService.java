package com.ruoyi.system.crm.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.crm.domain.CrmCustomerBehavior;
import com.ruoyi.system.crm.mapper.CrmCustomerBehaviorMapper;
import com.ruoyi.system.crm.support.CrmBehaviorTaskManager;

@Service
public class CrmCustomerBehaviorAsyncService
{
    private static final Logger log = LoggerFactory.getLogger(CrmCustomerBehaviorAsyncService.class);
    private static final int BATCH_SIZE = 500;
    private static final String[] DATE_PATTERNS = {
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd"
    };

    @Autowired
    private CrmCustomerBehaviorMapper crmCustomerBehaviorMapper;

    @Autowired
    private CrmBehaviorTaskManager taskManager;

    @Async("threadPoolTaskExecutor")
    public void generateAsync(String taskId, int count)
    {
        try
        {
            Random random = new Random();
            int processed = 0;
            while (processed < count)
            {
                int batchCount = Math.min(BATCH_SIZE, count - processed);
                List<CrmCustomerBehavior> batch = new ArrayList<>(batchCount);
                for (int i = 0; i < batchCount; i++)
                {
                    CrmCustomerBehavior row = new CrmCustomerBehavior();
                    row.setCustomerId((long) (random.nextInt(500) + 1));
                    String type = CrmCustomerBehaviorServiceImpl.BEHAVIOR_TYPES[random.nextInt(CrmCustomerBehaviorServiceImpl.BEHAVIOR_TYPES.length)];
                    row.setBehaviorType(type);
                    row.setDescription(type + "记录-" + (processed + i + 1));
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_YEAR, -random.nextInt(365));
                    row.setBehaviorTime(cal.getTime());
                    batch.add(row);
                }
                assignIdsAndInsert(batch);
                processed += batchCount;
                taskManager.updateProgress(taskId, processed);
            }
            taskManager.markDone(taskId);
        }
        catch (Exception e)
        {
            log.error("行为数据生成失败 taskId={}", taskId, e);
            taskManager.markFailed(taskId, e.getMessage());
        }
    }

    @Async("threadPoolTaskExecutor")
    public void importExcelAsync(String taskId, String filePath)
    {
        File file = new File(filePath);
        try (Workbook workbook = WorkbookFactory.create(file))
        {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null)
            {
                throw new ServiceException("Excel 无有效工作表");
            }
            DataFormatter formatter = new DataFormatter();
            int total = countDataRows(sheet, formatter);
            taskManager.updateTotal(taskId, total);
            List<CrmCustomerBehavior> batch = new ArrayList<>(BATCH_SIZE);
            int processed = 0;
            int lastRowNum = sheet.getLastRowNum();
            for (int i = 1; i <= lastRowNum; i++)
            {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row, formatter))
                {
                    continue;
                }
                batch.add(parseRow(row, formatter, i + 1));
                if (batch.size() >= BATCH_SIZE)
                {
                    assignIdsAndInsert(batch);
                    processed += batch.size();
                    taskManager.updateProgress(taskId, processed);
                    batch.clear();
                }
            }
            if (!batch.isEmpty())
            {
                assignIdsAndInsert(batch);
                processed += batch.size();
                taskManager.updateProgress(taskId, processed);
            }
            taskManager.markDone(taskId);
        }
        catch (Exception e)
        {
            log.error("行为数据 Excel 导入失败 taskId={}", taskId, e);
            taskManager.markFailed(taskId, e.getMessage());
        }
        finally
        {
            try
            {
                Files.deleteIfExists(file.toPath());
            }
            catch (IOException ignored)
            {
            }
        }
    }

    private void assignIdsAndInsert(List<CrmCustomerBehavior> batch)
    {
        List<Long> ids = crmCustomerBehaviorMapper.selectNextIds(batch.size());
        for (int i = 0; i < batch.size(); i++)
        {
            batch.get(i).setId(ids.get(i));
        }
        crmCustomerBehaviorMapper.batchInsert(batch);
    }

    private int countDataRows(Sheet sheet, DataFormatter formatter)
    {
        int total = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++)
        {
            Row row = sheet.getRow(i);
            if (row != null && !isEmptyRow(row, formatter))
            {
                total++;
            }
        }
        return total;
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter)
    {
        for (int c = 0; c < 4; c++)
        {
            if (StringUtils.isNotEmpty(formatter.formatCellValue(row.getCell(c)).trim()))
            {
                return false;
            }
        }
        return true;
    }

    private CrmCustomerBehavior parseRow(Row row, DataFormatter formatter, int rowNum) throws ParseException
    {
        String customerIdStr = formatter.formatCellValue(row.getCell(0)).trim();
        String behaviorType = formatter.formatCellValue(row.getCell(1)).trim();
        String description = formatter.formatCellValue(row.getCell(2)).trim();
        String behaviorTimeStr = formatter.formatCellValue(row.getCell(3)).trim();

        if (StringUtils.isEmpty(customerIdStr))
        {
            throw new ParseException("第 " + rowNum + " 行：客户ID不能为空", rowNum);
        }
        if (StringUtils.isEmpty(behaviorType))
        {
            throw new ParseException("第 " + rowNum + " 行：行为类型不能为空", rowNum);
        }
        CrmCustomerBehavior behavior = new CrmCustomerBehavior();
        try
        {
            behavior.setCustomerId(Long.parseLong(customerIdStr));
        }
        catch (NumberFormatException e)
        {
            throw new ParseException("第 " + rowNum + " 行：客户ID须为数字", rowNum);
        }
        behavior.setBehaviorType(behaviorType);
        behavior.setDescription(StringUtils.isNotEmpty(description) ? description : behaviorType + "记录-" + rowNum);
        behavior.setBehaviorTime(StringUtils.isNotEmpty(behaviorTimeStr) ? parseDate(behaviorTimeStr) : new Date());
        return behavior;
    }

    private Date parseDate(String value) throws ParseException
    {
        for (String pattern : DATE_PATTERNS)
        {
            try
            {
                return new SimpleDateFormat(pattern).parse(value);
            }
            catch (ParseException ignored)
            {
            }
        }
        throw new ParseException("行为时间格式不正确：" + value, 0);
    }
}
