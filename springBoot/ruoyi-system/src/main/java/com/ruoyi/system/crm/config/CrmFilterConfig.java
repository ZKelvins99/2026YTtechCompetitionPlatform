package com.ruoyi.system.crm.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ruoyi.system.crm.filter.CrmTraceIdFilter;

@Configuration
public class CrmFilterConfig
{
    @Bean
    public FilterRegistrationBean<CrmTraceIdFilter> crmTraceIdFilterRegistration(CrmTraceIdFilter filter)
    {
        FilterRegistrationBean<CrmTraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/crm/*");
        registration.setName("crmTraceIdFilter");
        registration.setOrder(1);
        return registration;
    }
}
