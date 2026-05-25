package com.ruoyi.system.crm.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.system.crm.support.CrmApiMarketGuard;
import jakarta.servlet.http.HttpServletRequest;

/**
 * API 服务市场下架切面：已登记且 status=0 的接口，任意调用方式均直接返回「接口已下架」。
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CrmApiMarketOfflineAspect
{
    @Autowired
    private CrmApiMarketGuard crmApiMarketGuard;

    @Pointcut("execution(* com.ruoyi.system.crm.controller..*(..))")
    public void crmControllerPointcut()
    {
    }

    @Around("crmControllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable
    {
        HttpServletRequest request = ServletUtils.getRequest();
        String offlineMsg = crmApiMarketGuard.checkOfflineMessage(request);
        if (offlineMsg != null)
        {
            throw new ServiceException(offlineMsg);
        }
        return joinPoint.proceed();
    }
}
