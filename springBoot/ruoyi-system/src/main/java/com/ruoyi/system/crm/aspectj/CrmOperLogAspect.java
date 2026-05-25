package com.ruoyi.system.crm.aspectj;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.filter.PropertyPreExcludeFilter;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.crm.context.CrmTraceContext;
import com.ruoyi.system.crm.domain.CrmOperLog;
import com.ruoyi.system.crm.mapper.CrmOperLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * CRM 操作日志 AOP 切面
 */
@Aspect
@Component
public class CrmOperLogAspect
{
    private static final int MAX_LEN = 4000;

    @Autowired
    private CrmOperLogMapper crmOperLogMapper;

    @Around("execution(* com.ruoyi.system.crm.controller..*(..)) "
        + "&& !execution(* com.ruoyi.system.crm.controller.CrmOperLogController.*(..)) "
        + "&& !execution(* com.ruoyi.system.crm.controller.CrmDashboardController.*(..)) "
        + "&& !execution(* com.ruoyi.system.crm.controller.CrmMessageRecordController.unreadCount(..)) "
        + "&& !execution(* com.ruoyi.system.crm.controller.CrmMessageRecordController.unreadList(..)) "
        + "&& !execution(* com.ruoyi.system.crm.controller.CrmCustomerBehaviorController.getTask(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable
    {
        long start = System.currentTimeMillis();
        CrmOperLog log = new CrmOperLog();
        HttpServletRequest request = ServletUtils.getRequest();
        if (request != null)
        {
            log.setRequestUrl(StringUtils.substring(request.getRequestURI(), 0, 200));
            log.setRequestMethod(request.getMethod());
        }
        log.setTraceId(CrmTraceContext.getTraceId());
        log.setRequestParams(truncate(argsToJson(joinPoint.getArgs())));
        try
        {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser != null)
            {
                log.setOperator(loginUser.getUsername());
            }
        }
        catch (Exception ignored) { }

        Object result = null;
        try
        {
            result = joinPoint.proceed();
            log.setStatus("0");
            log.setResponseResult(truncate(JSON.toJSONString(result)));
            return result;
        }
        catch (Throwable e)
        {
            log.setStatus("1");
            log.setErrorMsg(StringUtils.substring(e.getMessage(), 0, 2000));
            throw e;
        }
        finally
        {
            log.setCostMillis(System.currentTimeMillis() - start);
            String sqlJson = CrmTraceContext.getSqlStatementsJson();
            log.setSqlStatements(sqlJson);
            try
            {
                crmOperLogMapper.insertCrmOperLog(log);
            }
            catch (Exception e)
            {
                org.slf4j.LoggerFactory.getLogger(CrmOperLogAspect.class)
                    .warn("CRM 操作日志入库失败 url={}", log.getRequestUrl(), e);
            }
        }
    }

    private String argsToJson(Object[] args)
    {
        if (args == null || args.length == 0) return "";
        try
        {
            Object[] filtered = new Object[args.length];
            for (int i = 0; i < args.length; i++)
            {
                Object arg = args[i];
                if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
                    || arg instanceof MultipartFile)
                {
                    filtered[i] = arg.getClass().getSimpleName();
                }
                else
                {
                    filtered[i] = arg;
                }
            }
            return JSON.toJSONString(filtered, new PropertyPreExcludeFilter().addExcludes("password"));
        }
        catch (Exception e)
        {
            return "";
        }
    }

    private String truncate(String s)
    {
        if (s == null) return null;
        return s.length() > MAX_LEN ? s.substring(0, MAX_LEN) : s;
    }
}
