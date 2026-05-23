package com.ruoyi.system.crm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.crm.domain.CrmOpportunity;

/**
 * 商机Mapper接口
 */
public interface CrmOpportunityMapper
{
    CrmOpportunity selectCrmOpportunityById(Long id);

    List<CrmOpportunity> selectCrmOpportunityList(CrmOpportunity opportunity);

    int countByNameAndCustomer(@Param("opportunityName") String opportunityName, @Param("customerId") Long customerId);

    int insertCrmOpportunity(CrmOpportunity opportunity);

    int updateCrmOpportunity(CrmOpportunity opportunity);

    int deleteCrmOpportunityById(Long id);

    int deleteCrmOpportunityByIds(Long[] ids);
}
