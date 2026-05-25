package com.ruoyi.system.crm.support;

import java.util.function.LongSupplier;
import org.springframework.stereotype.Component;

/**
 * 行为数据总量缓存，避免滚动加载每条请求全表 count
 */
@Component
public class CrmBehaviorStatsCache
{
    /** 总量展示缓存，避免频繁全表 COUNT */
    private static final long TTL_MS = 300_000;

    private volatile long cachedTotal = -1;
    private volatile long cachedAt;

    public long getTotal(LongSupplier loader)
    {
        long now = System.currentTimeMillis();
        if (cachedTotal >= 0 && now - cachedAt < TTL_MS)
        {
            return cachedTotal;
        }
        synchronized (this)
        {
            if (cachedTotal >= 0 && now - cachedAt < TTL_MS)
            {
                return cachedTotal;
            }
            cachedTotal = loader.getAsLong();
            cachedAt = System.currentTimeMillis();
            return cachedTotal;
        }
    }

    public void invalidate()
    {
        cachedTotal = -1;
        cachedAt = 0;
    }

    /** 导入/生成结束后写入精确总量，避免下一屏再走全表 COUNT */
    public void setCachedTotal(long total)
    {
        cachedTotal = total;
        cachedAt = System.currentTimeMillis();
    }
}
