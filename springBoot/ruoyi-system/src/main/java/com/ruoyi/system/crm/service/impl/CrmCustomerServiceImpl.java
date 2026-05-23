package com.ruoyi.system.crm.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.crm.domain.CrmCustomer;
import com.ruoyi.system.crm.mapper.CrmContactMapper;
import com.ruoyi.system.crm.mapper.CrmCustomerMapper;
import com.ruoyi.system.crm.service.ICrmCustomerService;
import com.ruoyi.system.crm.support.CrmProfileRefreshHelper;

/**
 * 客户Service业务层处理
 */
@Service
public class CrmCustomerServiceImpl implements ICrmCustomerService
{
    @Autowired
    private CrmCustomerMapper crmCustomerMapper;

    @Autowired
    private CrmContactMapper crmContactMapper;

    @Autowired
    private CrmProfileRefreshHelper profileRefreshHelper;

    @Override
    public CrmCustomer selectCrmCustomerById(Long id)
    {
        CrmCustomer customer = crmCustomerMapper.selectCrmCustomerById(id);
        if (customer != null)
        {
            customer.setContacts(crmContactMapper.selectCrmContactByCustomerId(id));
        }
        return customer;
    }

    @Override
    public List<CrmCustomer> selectCrmCustomerList(CrmCustomer customer)
    {
        return crmCustomerMapper.selectCrmCustomerList(customer);
    }

    @Override
    public int insertCrmCustomer(CrmCustomer customer)
    {
        if (customer.getCustomerNo() == null || customer.getCustomerNo().isEmpty())
        {
            customer.setCustomerNo(generateCustomerNo());
        }
        int rows = crmCustomerMapper.insertCrmCustomer(customer);
        profileRefreshHelper.refreshCurrentUser();
        return rows;
    }

    @Override
    public int updateCrmCustomer(CrmCustomer customer)
    {
        int rows = crmCustomerMapper.updateCrmCustomer(customer);
        profileRefreshHelper.refreshCurrentUser();
        return rows;
    }

    @Override
    public int deleteCrmCustomerByIds(Long[] ids)
    {
        int rows = crmCustomerMapper.deleteCrmCustomerByIds(ids);
        profileRefreshHelper.refreshCurrentUser();
        return rows;
    }

    private String generateCustomerNo()
    {
        return "CUS" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
            + String.format("%04d", (int) (Math.random() * 10000));
    }
}
