package com.ruoyi.system.crm.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.crm.domain.CrmUserProfile;

public interface CrmUserProfileMapper
{
    CrmUserProfile selectByUserId(Long userId);

    int insertCrmUserProfile(CrmUserProfile profile);

    int updateCrmUserProfile(CrmUserProfile profile);

    int countCustomerByCreateBy(@Param("createBy") String createBy);

    int countOpportunityByCreateBy(@Param("createBy") String createBy);

    int countWinOpportunityByCreateBy(@Param("createBy") String createBy);

    BigDecimal sumContractAmountByCreateBy(@Param("createBy") String createBy);

    int countActiveContractByCreateBy(@Param("createBy") String createBy);

    int countContractByCreateBy(@Param("createBy") String createBy);

    int countRecentOperLogByOperator(@Param("operator") String operator);

    List<Map<String, Object>> selectIndustryStats(@Param("createBy") String createBy);

    List<Map<String, Object>> selectStageStats(@Param("createBy") String createBy);
}
