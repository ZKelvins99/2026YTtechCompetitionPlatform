package com.ruoyi.system.crm.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.crm.config.CrmBehaviorImportProperties;
import com.ruoyi.system.crm.domain.CrmBehaviorTaskStatus;
import com.ruoyi.system.crm.domain.CrmCustomerBehavior;
import com.ruoyi.system.crm.domain.CrmCustomerBehaviorConstants;
import com.ruoyi.system.crm.support.CrmBehaviorBulkInserter;
import com.ruoyi.system.crm.support.CrmBehaviorEasyExcelImporter;
import com.ruoyi.system.crm.support.CrmBehaviorStatsCache;
import com.ruoyi.system.crm.support.CrmBehaviorTaskManager;

/**
 * 行为数据异步导入/生成：
 * EasyExcel 单线程读 + 批内并行解析 + 多线程 JDBC 落库（每批独立事务）。
 */
@Service
public class CrmCustomerBehaviorAsyncService
{
    private static final Logger log = LoggerFactory.getLogger(CrmCustomerBehaviorAsyncService.class);

    private static final List<CrmCustomerBehavior> END_SIGNAL = Collections.emptyList();

    @Autowired
    private CrmBehaviorBulkInserter bulkInserter;

    @Autowired
    private CrmBehaviorTaskManager taskManager;

    @Autowired
    private CrmBehaviorStatsCache statsCache;

    @Autowired
    private CrmBehaviorImportProperties importProperties;

    @Autowired
    @Qualifier("crmBehaviorImportExecutor")
    private ThreadPoolTaskExecutor crmBehaviorImportExecutor;

    @Async("threadPoolTaskExecutor")
    public void generateAsync(String taskId, int count)
    {
        try
        {
            int batchSize = importProperties.getBatchSize();
            int progressStep = importProperties.getProgressStep();
            AtomicInteger processed = new AtomicInteger(0);
            Random random = new Random();
            runInsertPipeline(taskId, progressStep, processed, (queue, failed, failMessage) -> {
                int done = 0;
                while (done < count && !failed.get())
                {
                    int chunk = Math.min(batchSize, count - done);
                    List<CrmCustomerBehavior> batch = new ArrayList<>(chunk);
                    for (int i = 0; i < chunk; i++)
                    {
                        CrmCustomerBehavior row = new CrmCustomerBehavior();
                        row.setCustomerId((long) (random.nextInt(500) + 1));
                        String type = CrmCustomerBehaviorConstants.BEHAVIOR_TYPES[
                            random.nextInt(CrmCustomerBehaviorConstants.BEHAVIOR_TYPES.length)];
                        row.setBehaviorType(type);
                        row.setDescription(type + "记录-" + (done + i + 1));
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.DAY_OF_YEAR, -random.nextInt(365));
                        row.setBehaviorTime(cal.getTime());
                        batch.add(row);
                    }
                    queue.put(batch);
                    done += chunk;
                }
            });
            statsCache.addImported(processed.get());
        }
        catch (Exception e)
        {
            log.error("行为数据生成失败 taskId={}", taskId, e);
            markFailedIfRunning(taskId, e.getMessage());
        }
    }

    private void markFailedIfRunning(String taskId, String message)
    {
        CrmBehaviorTaskStatus current = taskManager.getTask(taskId);
        if (current != null && "RUNNING".equals(current.getStatus()))
        {
            taskManager.markFailed(taskId, message);
        }
    }

    @Async("threadPoolTaskExecutor")
    public void importExcelAsync(String taskId, String filePath)
    {
        File file = new File(filePath);
        try
        {
            crmBehaviorImportExecutor.execute(() -> {
                try
                {
                    int total = CrmBehaviorEasyExcelImporter.countValidRows(file);
                    if (total > 0)
                    {
                        taskManager.updateTotal(taskId, total);
                    }
                }
                catch (Exception e)
                {
                    log.warn("并行统计 Excel 行数失败 taskId={}", taskId, e);
                }
            });

            int batchSize = importProperties.getBatchSize();
            int progressStep = importProperties.getProgressStep();
            boolean parallelParse = importProperties.isParallelParse();
            AtomicInteger processed = new AtomicInteger(0);

            runInsertPipeline(taskId, progressStep, processed, (queue, failed, failMessage) -> {
                CrmBehaviorEasyExcelImporter.readInBatches(file, batchSize, parallelParse, batch -> {
                    if (failed.get())
                    {
                        return;
                    }
                    try
                    {
                        queue.put(batch);
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        failed.set(true);
                        failMessage.compareAndSet(null, "导入被中断");
                    }
                });
            });

            int finalProcessed = processed.get();
            if (finalProcessed <= 0)
            {
                throw new ServiceException("未成功写入任何数据，请检查 Excel 内容或数据库连接");
            }
            CrmBehaviorTaskStatus task = taskManager.getTask(taskId);
            if (task == null || task.getTotal() <= 0)
            {
                taskManager.updateTotal(taskId, finalProcessed);
            }
            statsCache.addImported(finalProcessed);
        }
        catch (ServiceException e)
        {
            log.error("行为数据 Excel 导入失败 taskId={}", taskId, e);
            markFailedIfRunning(taskId, e.getMessage());
        }
        catch (Exception e)
        {
            log.error("行为数据 Excel 导入失败 taskId={}", taskId, e);
            markFailedIfRunning(taskId, e.getMessage());
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

    private void runInsertPipeline(String taskId, int progressStep, AtomicInteger processed,
        BatchProducer producer) throws Exception
    {
        int consumerThreads = importProperties.getConsumerThreads();
        int queueCapacity = importProperties.getQueueBatchCapacity();
        BlockingQueue<List<CrmCustomerBehavior>> queue = new LinkedBlockingQueue<>(queueCapacity);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicReference<String> failMessage = new AtomicReference<>();
        CountDownLatch consumersDone = new CountDownLatch(consumerThreads);

        for (int i = 0; i < consumerThreads; i++)
        {
            crmBehaviorImportExecutor.execute(() ->
                consumeInsertBatches(taskId, queue, processed, progressStep, failed, failMessage, consumersDone));
        }

        boolean finished = false;
        try
        {
            producer.produce(queue, failed, failMessage);
            for (int i = 0; i < consumerThreads; i++)
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
            taskManager.updateProgress(taskId, processed.get());
            taskManager.markDone(taskId);
            finished = true;
        }
        catch (Exception e)
        {
            failed.set(true);
            for (int i = 0; i < consumerThreads; i++)
            {
                queue.offer(END_SIGNAL);
            }
            if (!finished)
            {
                CrmBehaviorTaskStatus current = taskManager.getTask(taskId);
                if (current != null && "RUNNING".equals(current.getStatus()))
                {
                    taskManager.markFailed(taskId, e.getMessage());
                }
            }
            throw e;
        }
    }

    private void consumeInsertBatches(String taskId, BlockingQueue<List<CrmCustomerBehavior>> queue,
        AtomicInteger processed, int progressStep, AtomicBoolean failed,
        AtomicReference<String> failMessage, CountDownLatch consumersDone)
    {
        int lastReported = 0;
        try
        {
            while (!failed.get())
            {
                List<CrmCustomerBehavior> batch;
                try
                {
                    batch = queue.take();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    failed.set(true);
                    failMessage.compareAndSet(null, "导入被中断");
                    break;
                }
                if (batch == END_SIGNAL)
                {
                    break;
                }
                if (batch.isEmpty())
                {
                    continue;
                }
                try
                {
                    bulkInserter.importBatchInTransaction(batch);
                    int p = processed.addAndGet(batch.size());
                    if (p - lastReported >= progressStep)
                    {
                        lastReported = p;
                        taskManager.updateProgress(taskId, p);
                    }
                }
                catch (SQLException e)
                {
                    failed.set(true);
                    failMessage.compareAndSet(null, toUserMessage(e));
                    log.error("行为数据批量写入失败 taskId={}", taskId, e);
                    break;
                }
                catch (Exception e)
                {
                    failed.set(true);
                    failMessage.compareAndSet(null, e.getMessage());
                    log.error("行为数据批量写入失败 taskId={}", taskId, e);
                    break;
                }
            }
        }
        finally
        {
            consumersDone.countDown();
        }
    }

    private static String toUserMessage(SQLException e)
    {
        String msg = e.getMessage();
        if (msg != null && (msg.contains("套接字") || msg.contains("socket") || msg.contains("Socket")))
        {
            return "数据库连接超时或中断，请稍后重试";
        }
        return StringUtils.isNotEmpty(msg) ? msg : "数据库写入失败";
    }
}
