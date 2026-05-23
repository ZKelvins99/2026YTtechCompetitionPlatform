package com.ruoyi.system.crm.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.crm.domain.CrmWorkbenchLayout;

public interface CrmWorkbenchMapper
{
    CrmWorkbenchLayout selectByUserId(Long userId);

    int insertLayout(CrmWorkbenchLayout layout);

    int updateLayout(CrmWorkbenchLayout layout);

    List<Map<String, Object>> selectPendingApprovals(@Param("userId") Long userId);

    int countCustomerByCreateBy(@Param("createBy") String createBy);

    int countCustomerThisMonthByCreateBy(@Param("createBy") String createBy);

    List<Map<String, Object>> selectOpportunityStageStats(@Param("createBy") String createBy);

    List<Map<String, Object>> selectExpiringContracts();
}
