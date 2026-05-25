package com.ruoyi.system.crm.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.crm.domain.CrmApiDebugRequest;
import com.ruoyi.system.crm.domain.CrmApiInfo;
import com.ruoyi.system.crm.mapper.CrmApiInfoMapper;
import com.ruoyi.system.crm.mapper.CrmCustomerMapper;
import com.ruoyi.system.crm.service.ICrmApiInfoService;
import com.ruoyi.system.crm.support.CrmApiMarketGuard;

@Service
public class CrmApiInfoServiceImpl implements ICrmApiInfoService
{
    @Autowired
    private CrmApiInfoMapper crmApiInfoMapper;

    @Autowired
    private CrmCustomerMapper crmCustomerMapper;

    @Autowired
    private RestTemplate crmRestTemplate;

    @Autowired
    private CrmApiMarketGuard crmApiMarketGuard;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Override
    public CrmApiInfo selectCrmApiInfoById(Long id)
    {
        return crmApiInfoMapper.selectCrmApiInfoById(id);
    }

    @Override
    public List<CrmApiInfo> selectCrmApiInfoList(CrmApiInfo apiInfo)
    {
        return crmApiInfoMapper.selectCrmApiInfoList(apiInfo);
    }

    @Override
    public int insertCrmApiInfo(CrmApiInfo apiInfo)
    {
        normalizeApiInfo(apiInfo);
        if (StringUtils.isEmpty(apiInfo.getStatus()))
        {
            apiInfo.setStatus("1");
        }
        return crmApiInfoMapper.insertCrmApiInfo(apiInfo);
    }

    @Override
    public int updateCrmApiInfo(CrmApiInfo apiInfo)
    {
        normalizeApiInfo(apiInfo);
        return crmApiInfoMapper.updateCrmApiInfo(apiInfo);
    }

    private void normalizeApiInfo(CrmApiInfo apiInfo)
    {
        if (apiInfo == null)
        {
            return;
        }
        apiInfo.setApiUrl(CrmApiMarketGuard.normalizeApiUrl(apiInfo.getApiUrl()));
        if (StringUtils.isNotEmpty(apiInfo.getApiMethod()))
        {
            apiInfo.setApiMethod(apiInfo.getApiMethod().trim().toUpperCase());
        }
    }

    @Override
    public int deleteCrmApiInfoByIds(Long[] ids)
    {
        return crmApiInfoMapper.deleteCrmApiInfoByIds(ids);
    }

    @Override
    public int online(Long id)
    {
        return crmApiInfoMapper.updateStatus(id, "1");
    }

    @Override
    public int offline(Long id)
    {
        return crmApiInfoMapper.updateStatus(id, "0");
    }

    @Override
    public Map<String, Object> debugApi(Long id, CrmApiDebugRequest request, String authorization)
    {
        CrmApiInfo apiInfo = crmApiInfoMapper.selectCrmApiInfoById(id);
        if (apiInfo == null)
        {
            throw new ServiceException("API 不存在");
        }
        if (crmApiMarketGuard.isOffline(apiInfo.getId()))
        {
            throw new ServiceException(CrmApiMarketGuard.OFFLINE_MESSAGE);
        }

        String targetUrl = resolveUrl(apiInfo.getApiUrl());
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.isNotEmpty(authorization))
        {
            headers.set("Authorization", authorization);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpMethod method = HttpMethod.valueOf(apiInfo.getApiMethod().toUpperCase());
        String body = request != null ? request.getRequestBody() : null;
        HttpEntity<String> entity = new HttpEntity<>(StringUtils.isNotEmpty(body) ? body : null, headers);

        long start = System.currentTimeMillis();
        try
        {
            ResponseEntity<String> response = crmRestTemplate.exchange(targetUrl, method, entity, String.class);
            long cost = System.currentTimeMillis() - start;
            Map<String, Object> result = new HashMap<>();
            result.put("statusCode", response.getStatusCode().value());
            result.put("responseBody", response.getBody());
            result.put("responseTime", cost);
            return result;
        }
        catch (Exception e)
        {
            long cost = System.currentTimeMillis() - start;
            Map<String, Object> result = new HashMap<>();
            result.put("statusCode", 500);
            result.put("responseBody", e.getMessage());
            result.put("responseTime", cost);
            return result;
        }
    }

    @Override
    public long getCustomerCount()
    {
        return crmCustomerMapper.countAll();
    }

    private String resolveUrl(String apiUrl)
    {
        if (apiUrl.startsWith("http://") || apiUrl.startsWith("https://"))
        {
            return apiUrl;
        }
        String path = apiUrl.startsWith("/") ? apiUrl : "/" + apiUrl;
        String ctx = StringUtils.isEmpty(contextPath) ? "" : contextPath;
        return "http://127.0.0.1:" + serverPort + ctx + path;
    }
}
