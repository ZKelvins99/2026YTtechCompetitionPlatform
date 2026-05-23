package com.ruoyi.system.crm.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.crm.domain.CrmContract;
import com.ruoyi.system.crm.mapper.CrmContractMapper;
import com.ruoyi.system.crm.service.ICrmContractService;
import com.ruoyi.system.crm.support.CrmProfileRefreshHelper;

@Service
public class CrmContractServiceImpl implements ICrmContractService
{
    @Autowired
    private CrmContractMapper crmContractMapper;

    @Autowired
    private CrmProfileRefreshHelper profileRefreshHelper;

    @Override
    public CrmContract selectCrmContractById(Long id)
    {
        return crmContractMapper.selectCrmContractById(id);
    }

    @Override
    public List<CrmContract> selectCrmContractList(CrmContract contract)
    {
        return crmContractMapper.selectCrmContractList(contract);
    }

    @Override
    public int insertCrmContract(CrmContract contract)
    {
        if (contract.getContractNo() == null || contract.getContractNo().isEmpty())
        {
            contract.setContractNo("CT" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + String.format("%04d", (int) (Math.random() * 10000)));
        }
        if (contract.getStatusId() == null)
        {
            contract.setStatusId(crmContractMapper.selectStatusIdByCode("DRAFT"));
        }
        int rows = crmContractMapper.insertCrmContract(contract);
        profileRefreshHelper.refreshCurrentUser();
        return rows;
    }

    @Override
    public int updateCrmContract(CrmContract contract)
    {
        int rows = crmContractMapper.updateCrmContract(contract);
        profileRefreshHelper.refreshCurrentUser();
        return rows;
    }

    @Override
    public int deleteCrmContractByIds(Long[] ids)
    {
        int rows = crmContractMapper.deleteCrmContractByIds(ids);
        profileRefreshHelper.refreshCurrentUser();
        return rows;
    }
}
