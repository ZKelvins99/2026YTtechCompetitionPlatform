package com.ruoyi.system.crm.mapper;

import java.util.List;
import com.ruoyi.system.crm.domain.CrmDictItem;

/**
 * CRM码表Mapper接口
 */
public interface CrmDictMapper
{
    List<CrmDictItem> selectCustomerLevelList();

    List<CrmDictItem> selectOpportunityStageList();

    List<CrmDictItem> selectContactTypeList();

    List<CrmDictItem> selectContractStatusList();
}
