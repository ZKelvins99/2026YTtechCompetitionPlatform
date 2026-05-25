package com.ruoyi.system.crm.service;

import java.io.InputStream;
import java.util.List;
import com.ruoyi.system.crm.domain.CrmImportResult;
import com.ruoyi.system.crm.domain.CrmOpportunity;

/**
 * 商机Service接口
 */
public interface ICrmOpportunityService
{
    CrmOpportunity selectCrmOpportunityById(Long id);

    List<CrmOpportunity> selectCrmOpportunityList(CrmOpportunity opportunity);

    int insertCrmOpportunity(CrmOpportunity opportunity);

    int updateCrmOpportunity(CrmOpportunity opportunity);

    int deleteCrmOpportunityByIds(Long[] ids);

    CrmImportResult importOpportunity(InputStream inputStream, String operName);

    /**
     * 为 Excel 导入样例创建测试客户（已存在则跳过）
     */
    String seedImportTestCustomers(String operName);
}
