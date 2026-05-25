package com.ruoyi.system.crm.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.ruoyi.system.crm.support.CrmBehaviorImportSettings;

@Configuration
@EnableAsync
public class CrmAsyncConfig
{
    /**
     * CRM 行为数据导入专用线程池：核心数 = {@link CrmBehaviorImportSettings#CONSUMER_THREADS}。
     */
    @Bean(name = "crmBehaviorImportExecutor")
    public ThreadPoolTaskExecutor crmBehaviorImportExecutor()
    {
        int consumers = CrmBehaviorImportSettings.CONSUMER_THREADS;
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(consumers);
        executor.setMaxPoolSize(consumers + 2);
        executor.setQueueCapacity(64);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("crm-behavior-import-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
