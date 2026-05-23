package com.ruoyi.system.crm.mapper;

import java.util.List;
import com.ruoyi.system.crm.domain.CrmCustomer;

/**
 * 客户Mapper接口
 */
public interface CrmCustomerMapper
{
    CrmCustomer selectCrmCustomerById(Long id);

    List<CrmCustomer> selectCrmCustomerList(CrmCustomer customer);

    int insertCrmCustomer(CrmCustomer customer);

    int updateCrmCustomer(CrmCustomer customer);

    int deleteCrmCustomerById(Long id);

    int deleteCrmCustomerByIds(Long[] ids);
}
