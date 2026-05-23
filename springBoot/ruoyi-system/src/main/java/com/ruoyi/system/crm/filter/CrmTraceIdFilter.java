package com.ruoyi.system.crm.filter;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ruoyi.system.crm.context.CrmTraceContext;

/**
 * 生成 traceId 并写入响应 Header
 */
@Component
public class CrmTraceIdFilter extends OncePerRequestFilter
{
    public static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException
    {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        CrmTraceContext.setTraceId(traceId);
        response.setHeader(TRACE_HEADER, traceId);
        try
        {
            chain.doFilter(request, response);
        }
        finally
        {
            CrmTraceContext.clear();
        }
    }
}
