package com.ruoyi.system.crm.service;

import java.util.List;
import com.ruoyi.system.crm.domain.CrmCustomer;

/**
 * 客户Service接口
 */
public interface ICrmCustomerService
{
    CrmCustomer selectCrmCustomerById(Long id);

    List<CrmCustomer> selectCrmCustomerList(CrmCustomer customer);

    int insertCrmCustomer(CrmCustomer customer);

    int updateCrmCustomer(CrmCustomer customer);

    int deleteCrmCustomerByIds(Long[] ids);
}
