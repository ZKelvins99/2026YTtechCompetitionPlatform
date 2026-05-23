package com.ruoyi.system.crm.service;

import java.util.List;
import com.ruoyi.system.crm.domain.CrmDictItem;

/**
 * CRM码表Service接口
 */
public interface ICrmDictService
{
    List<CrmDictItem> selectDictByType(String type);
}
