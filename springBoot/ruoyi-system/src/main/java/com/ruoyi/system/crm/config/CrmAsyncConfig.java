package com.ruoyi.system.crm.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class CrmAsyncConfig
{
    /**
     * CRM 行为导入：并行落库 + 后台统计总行数。
     */
    @Bean(name = "crmBehaviorImportExecutor")
    public ThreadPoolTaskExecutor crmBehaviorImportExecutor(CrmBehaviorImportProperties properties)
    {
        int workers = Math.max(2, properties.getConsumerThreads() + 1);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(workers);
        executor.setMaxPoolSize(workers + 2);
        executor.setQueueCapacity(64);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("crm-behavior-import-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
