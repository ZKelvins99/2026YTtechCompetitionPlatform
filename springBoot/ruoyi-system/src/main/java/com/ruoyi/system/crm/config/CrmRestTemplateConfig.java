package com.ruoyi.system.crm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CrmRestTemplateConfig
{
    @Bean
    public RestTemplate crmRestTemplate()
    {
        return new RestTemplate();
    }
}
