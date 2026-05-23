package com.ruoyi.system.crm.service.impl;

import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.crm.domain.CrmDictItem;
import com.ruoyi.system.crm.mapper.CrmDictMapper;
import com.ruoyi.system.crm.service.ICrmDictService;

/**
 * CRM码表Service业务层处理
 */
@Service
public class CrmDictServiceImpl implements ICrmDictService
{
    @Autowired
    private CrmDictMapper crmDictMapper;

    @Override
    public List<CrmDictItem> selectDictByType(String type)
    {
        if (StringUtils.isEmpty(type))
        {
            return Collections.emptyList();
        }
        switch (type)
        {
            case "customer_level":
                return crmDictMapper.selectCustomerLevelList();
            case "opportunity_stage":
                return crmDictMapper.selectOpportunityStageList();
            case "contact_type":
                return crmDictMapper.selectContactTypeList();
            case "contract_status":
                return crmDictMapper.selectContractStatusList();
            default:
                return Collections.emptyList();
        }
    }
}
