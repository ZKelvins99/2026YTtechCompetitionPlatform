package com.ruoyi.system.crm.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.crm.domain.CrmCustomerBehavior;
import com.ruoyi.system.crm.support.CrmBehaviorBulkInserter;
import com.ruoyi.system.crm.support.CrmBehaviorStatsCache;
import com.ruoyi.system.crm.support.CrmBehaviorTaskManager;

@Service
public class CrmCustomerBehaviorAsyncService
{
    private static final Logger log = LoggerFactory.getLogger(CrmCustomerBehaviorAsyncService.class);

    /** 并行落库消费者（每线程独占 JDBC 连接） */
    private static final int CONSUMER_THREADS = 4;
    private static final int QUEUE_CAPACITY = 32;
    private static final int PROGRESS_STEP = 5000;
    /** 结束哨兵（单例空列表，与业务 batch 引用区分） */
    private static final List<CrmCustomerBehavior> END_SIGNAL = Collections.emptyList();

    private static final String[] DATE_PATTERNS = {
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd"
    };

    @Autowired
    private CrmBehaviorBulkInserter bulkInserter;

    @Autowired
    private CrmBehaviorTaskManager taskManager;

    @Autowired
    private CrmBehaviorStatsCache statsCache;

    @Autowired
    @Qualifier("crmBehaviorImportExecutor")
    private ThreadPoolTaskExecutor crmBehaviorImportExecutor;

    @Async("threadPoolTaskExecutor")
    public void generateAsync(String taskId, int count)
    {
        runPipeline(taskId, false, (queue, failed, failMessage) -> produceGenerated(count, queue, failed, failMessage));
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
            if (total <= 0)
            {
                throw new ServiceException("Excel 中无有效数据行（已跳过表头）");
            }
            taskManager.updateTotal(taskId, total);
            runPipeline(taskId, true, (queue, failed, failMessage) ->
                produceFromSheet(sheet, formatter, queue, failed, failMessage));
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

    @FunctionalInterface
    private interface BatchProducer
    {
        void produce(BlockingQueue<List<CrmCustomerBehavior>> queue,
            AtomicBoolean failed, AtomicReference<String> failMessage) throws Exception;
    }

    private void runPipeline(String taskId, boolean importTask, BatchProducer producer)
    {
        BlockingQueue<List<CrmCustomerBehavior>> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        AtomicInteger processed = new AtomicInteger(0);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicReference<String> failMessage = new AtomicReference<>();
        CountDownLatch consumersDone = new CountDownLatch(CONSUMER_THREADS);

        for (int i = 0; i < CONSUMER_THREADS; i++)
        {
            crmBehaviorImportExecutor.execute(() -> consumeBatches(taskId, queue, processed, failed, failMessage, consumersDone));
        }

        try
        {
            producer.produce(queue, failed, failMessage);
            for (int i = 0; i < CONSUMER_THREADS; i++)
            {
                queue.put(END_SIGNAL);
            }
            if (!consumersDone.await(6, TimeUnit.HOURS))
            {
                throw new ServiceException("导入超时");
            }
            if (failed.get())
            {
                throw new ServiceException(StringUtils.isNotEmpty(failMessage.get())
                    ? failMessage.get() : "导入失败");
            }
            int finalProcessed = processed.get();
            if (importTask && finalProcessed <= 0)
            {
                throw new ServiceException("未成功写入任何数据，请检查 Excel 内容或数据库连接");
            }
            taskManager.updateProgress(taskId, finalProcessed);
            taskManager.markDone(taskId);
            statsCache.addImported(finalProcessed);
            bulkInserter.clearIdPool();
        }
        catch (Exception e)
        {
            log.error("行为数据任务失败 taskId={}", taskId, e);
            taskManager.markFailed(taskId, e.getMessage());
            for (int i = 0; i < CONSUMER_THREADS; i++)
            {
                queue.offer(END_SIGNAL);
            }
        }
    }

    private void consumeBatches(String taskId, BlockingQueue<List<CrmCustomerBehavior>> queue,
        AtomicInteger processed, AtomicBoolean failed, AtomicReference<String> failMessage,
        CountDownLatch consumersDone)
    {
        CrmBehaviorBulkInserter.InsertSession session = null;
        try
        {
            session = bulkInserter.openSession();
            int lastReported = 0;
            while (true)
            {
                List<CrmCustomerBehavior> batch = queue.take();
                if (batch == END_SIGNAL)
                {
                    break;
                }
                if (batch.isEmpty())
                {
                    continue;
                }
                if (failed.get())
                {
                    break;
                }
                bulkInserter.insertBatch(session, batch);
                session.commit();
                int p = processed.addAndGet(batch.size());
                if (p - lastReported >= PROGRESS_STEP)
                {
                    lastReported = p;
                    taskManager.updateProgress(taskId, p);
                }
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            failed.set(true);
            failMessage.compareAndSet(null, "导入被中断");
        }
        catch (SQLException e)
        {
            failed.set(true);
            failMessage.compareAndSet(null, e.getMessage());
            log.error("行为数据批量写入失败 taskId={}", taskId, e);
        }
        catch (Exception e)
        {
            failed.set(true);
            failMessage.compareAndSet(null, e.getMessage());
            log.error("行为数据批量写入失败 taskId={}", taskId, e);
        }
        finally
        {
            if (session != null)
            {
                if (failed.get())
                {
                    session.rollback();
                }
                session.close();
            }
            consumersDone.countDown();
        }
    }

    private void produceFromSheet(Sheet sheet, DataFormatter formatter,
        BlockingQueue<List<CrmCustomerBehavior>> queue,
        AtomicBoolean failed, AtomicReference<String> failMessage) throws Exception
    {
        List<CrmCustomerBehavior> batch = new ArrayList<>(CrmBehaviorBulkInserter.BATCH_SIZE);
        int lastRowNum = sheet.getLastRowNum();
        for (int i = 1; i <= lastRowNum && !failed.get(); i++)
        {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row, formatter))
            {
                continue;
            }
            batch.add(parseRow(row, formatter, i + 1));
            if (batch.size() >= CrmBehaviorBulkInserter.BATCH_SIZE)
            {
                queue.put(new ArrayList<>(batch));
                batch.clear();
            }
        }
        if (!batch.isEmpty() && !failed.get())
        {
            queue.put(new ArrayList<>(batch));
        }
        if (failed.get())
        {
            throw new ServiceException(StringUtils.isNotEmpty(failMessage.get())
                ? failMessage.get() : "导入失败");
        }
    }

    private void produceGenerated(int count, BlockingQueue<List<CrmCustomerBehavior>> queue,
        AtomicBoolean failed, AtomicReference<String> failMessage) throws InterruptedException
    {
        Random random = new Random();
        int done = 0;
        while (done < count && !failed.get())
        {
            int batchCount = Math.min(CrmBehaviorBulkInserter.BATCH_SIZE, count - done);
            List<CrmCustomerBehavior> batch = new ArrayList<>(batchCount);
            for (int i = 0; i < batchCount; i++)
            {
                CrmCustomerBehavior row = new CrmCustomerBehavior();
                row.setCustomerId((long) (random.nextInt(500) + 1));
                String type = CrmCustomerBehaviorServiceImpl.BEHAVIOR_TYPES[
                    random.nextInt(CrmCustomerBehaviorServiceImpl.BEHAVIOR_TYPES.length)];
                row.setBehaviorType(type);
                row.setDescription(type + "记录-" + (done + i + 1));
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -random.nextInt(365));
                row.setBehaviorTime(cal.getTime());
                batch.add(row);
            }
            queue.put(batch);
            done += batchCount;
        }
        if (failed.get())
        {
            throw new ServiceException(failMessage.get());
        }
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
            behavior.setCustomerId(parseLongId(customerIdStr));
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
