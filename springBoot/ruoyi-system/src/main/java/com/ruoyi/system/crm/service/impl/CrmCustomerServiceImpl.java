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
        return crmCustomerMapper.insertCrmCustomer(customer);
    }

    @Override
    public int updateCrmCustomer(CrmCustomer customer)
    {
        return crmCustomerMapper.updateCrmCustomer(customer);
    }

    @Override
    public int deleteCrmCustomerByIds(Long[] ids)
    {
        return crmCustomerMapper.deleteCrmCustomerByIds(ids);
    }

    private String generateCustomerNo()
    {
        return "CUS" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
            + String.format("%04d", (int) (Math.random() * 10000));
    }
}
