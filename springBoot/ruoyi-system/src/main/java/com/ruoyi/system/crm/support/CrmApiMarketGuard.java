package com.ruoyi.system.crm.support;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.crm.domain.CrmApiInfo;
import com.ruoyi.system.crm.mapper.CrmApiInfoMapper;
import jakarta.servlet.http.HttpServletRequest;

/**
 * API 服务市场下架校验（供 AOP 与调试代理共用逻辑）
 */
@Component
public class CrmApiMarketGuard
{
    public static final String OFFLINE_MESSAGE = "接口已下架";

    /** 0=下架 1=上架 */
    public static final String STATUS_ONLINE = "1";

    @Autowired
    private CrmApiInfoMapper crmApiInfoMapper;

    /**
     * 若当前请求对应已登记且已下架的 API，返回下架提示；否则返回 null 表示可继续执行。
     */
    public String checkOfflineMessage(HttpServletRequest request)
    {
        if (request == null)
        {
            return null;
        }
        String apiMethod = request.getMethod();
        if (StringUtils.isEmpty(apiMethod))
        {
            return null;
        }
        String requestUrl = resolveRequestPath(request);
        if (StringUtils.isEmpty(requestUrl))
        {
            return null;
        }
        CrmApiInfo apiInfo = findRegisteredApi(requestUrl, apiMethod);
        if (apiInfo != null && !STATUS_ONLINE.equals(apiInfo.getStatus()))
        {
            return OFFLINE_MESSAGE;
        }
        return null;
    }

    public boolean isOffline(Long apiId)
    {
        if (apiId == null)
        {
            return false;
        }
        CrmApiInfo apiInfo = crmApiInfoMapper.selectCrmApiInfoById(apiId);
        return apiInfo != null && !STATUS_ONLINE.equals(apiInfo.getStatus());
    }

    /**
     * 按 URL + Method 查找登记记录（兼容有无前导 /、大小写等历史数据）
     */
    public CrmApiInfo findRegisteredApi(String requestUrl, String httpMethod)
    {
        if (StringUtils.isEmpty(requestUrl) || StringUtils.isEmpty(httpMethod))
        {
            return null;
        }
        String method = httpMethod.trim().toUpperCase();
        for (String candidate : urlCandidates(requestUrl))
        {
            CrmApiInfo apiInfo = crmApiInfoMapper.selectCrmApiInfoByUrlAndMethod(candidate, method);
            if (apiInfo != null)
            {
                return apiInfo;
            }
        }
        return null;
    }

    /**
     * 保存 API 时统一接口地址格式：/crm/xxx，无尾斜杠
     */
    public static String normalizeApiUrl(String apiUrl)
    {
        if (StringUtils.isEmpty(apiUrl))
        {
            return apiUrl;
        }
        String url = apiUrl.trim().replace('\\', '/');
        while (url.contains("//"))
        {
            url = url.replace("//", "/");
        }
        if (!url.startsWith("/"))
        {
            url = "/" + url;
        }
        if (url.length() > 1 && url.endsWith("/"))
        {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    public static String resolveRequestPath(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        if (StringUtils.isEmpty(uri))
        {
            uri = request.getServletPath();
        }
        if (StringUtils.isEmpty(uri))
        {
            return "";
        }
        String ctx = request.getContextPath();
        if (StringUtils.isNotEmpty(ctx) && uri.startsWith(ctx))
        {
            uri = uri.substring(ctx.length());
        }
        return normalizeApiUrl(uri);
    }

    private static List<String> urlCandidates(String normalizedUrl)
    {
        Set<String> set = new LinkedHashSet<>();
        String norm = normalizeApiUrl(normalizedUrl);
        if (StringUtils.isEmpty(norm))
        {
            return new ArrayList<>();
        }
        set.add(norm);
        if (norm.startsWith("/"))
        {
            set.add(norm.substring(1));
        }
        else
        {
            set.add("/" + norm);
        }
        return new ArrayList<>(set);
    }
}
