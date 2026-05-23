package com.ruoyi.system.crm.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.crm.domain.CrmApiDebugRequest;
import com.ruoyi.system.crm.domain.CrmApiInfo;

public interface ICrmApiInfoService
{
    CrmApiInfo selectCrmApiInfoById(Long id);

    List<CrmApiInfo> selectCrmApiInfoList(CrmApiInfo apiInfo);

    int insertCrmApiInfo(CrmApiInfo apiInfo);

    int updateCrmApiInfo(CrmApiInfo apiInfo);

    int deleteCrmApiInfoByIds(Long[] ids);

    int online(Long id);

    int offline(Long id);

    Map<String, Object> debugApi(Long id, CrmApiDebugRequest request, String authorization);

    long getCustomerCount();
}
