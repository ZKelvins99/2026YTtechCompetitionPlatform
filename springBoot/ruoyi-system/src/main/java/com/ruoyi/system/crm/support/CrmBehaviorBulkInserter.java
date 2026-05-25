package com.ruoyi.system.crm.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.system.crm.domain.CrmCustomerBehavior;
import com.ruoyi.system.crm.mapper.CrmCustomerBehaviorMapper;

/**
 * 线程安全的行为数据批量写入（预取序列 ID，减少数据库往返）
 */
@Component
public class CrmBehaviorBulkInserter
{
    /** Oracle INSERT ALL 建议单批不超过 1000 */
    public static final int BATCH_SIZE = 1000;

    private static final int ID_PREFETCH_SIZE = 3000;

    private final Object insertLock = new Object();

    private final ConcurrentLinkedQueue<Long> idPool = new ConcurrentLinkedQueue<>();

    @Autowired
    private CrmCustomerBehaviorMapper crmCustomerBehaviorMapper;

    /**
     * 批量写入（多线程可并发调用，内部串行落库保证线程安全）
     */
    public void insertBatch(List<CrmCustomerBehavior> batch)
    {
        if (batch == null || batch.isEmpty())
        {
            return;
        }
        synchronized (insertLock)
        {
            assignIds(batch);
            crmCustomerBehaviorMapper.batchInsert(batch);
        }
    }

    private void assignIds(List<CrmCustomerBehavior> batch)
    {
        int need = batch.size();
        while (idPool.size() < need)
        {
            refillIdPool(Math.max(ID_PREFETCH_SIZE, need - idPool.size()));
        }
        for (int i = 0; i < need; i++)
        {
            Long id = idPool.poll();
            if (id == null)
            {
                refillIdPool(need);
                id = idPool.poll();
            }
            batch.get(i).setId(id);
        }
    }

    private void refillIdPool(int count)
    {
        List<Long> ids = crmCustomerBehaviorMapper.selectNextIds(count);
        idPool.addAll(ids);
    }

    public void clearIdPool()
    {
        idPool.clear();
    }

    /** 将列表按 BATCH_SIZE 切分 */
    public static List<List<CrmCustomerBehavior>> partition(List<CrmCustomerBehavior> source)
    {
        List<List<CrmCustomerBehavior>> parts = new ArrayList<>();
        for (int i = 0; i < source.size(); i += BATCH_SIZE)
        {
            parts.add(new ArrayList<>(source.subList(i, Math.min(i + BATCH_SIZE, source.size()))));
        }
        return parts;
    }
}
