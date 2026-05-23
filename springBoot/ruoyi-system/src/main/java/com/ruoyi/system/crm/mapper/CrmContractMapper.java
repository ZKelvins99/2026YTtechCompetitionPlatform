package com.ruoyi.system.crm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.crm.domain.CrmContract;

public interface CrmContractMapper
{
    CrmContract selectCrmContractById(Long id);

    List<CrmContract> selectCrmContractList(CrmContract contract);

    int insertCrmContract(CrmContract contract);

    int updateCrmContract(CrmContract contract);

    int updateContractStatus(@Param("id") Long id, @Param("statusId") Long statusId);

    int deleteCrmContractByIds(Long[] ids);

    Long selectStatusIdByCode(@Param("statusCode") String statusCode);
}
