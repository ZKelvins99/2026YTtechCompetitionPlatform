package com.ruoyi.system.crm.service;

import java.util.List;
import com.ruoyi.system.crm.domain.CrmContract;

public interface ICrmContractService
{
    CrmContract selectCrmContractById(Long id);

    List<CrmContract> selectCrmContractList(CrmContract contract);

    int insertCrmContract(CrmContract contract);

    int updateCrmContract(CrmContract contract);

    int deleteCrmContractByIds(Long[] ids);
}
