package com.ruoyi.system.crm.mapper;

import java.util.List;
import com.ruoyi.system.crm.domain.CrmContact;

/**
 * 联系人Mapper接口
 */
public interface CrmContactMapper
{
    CrmContact selectCrmContactById(Long id);

    List<CrmContact> selectCrmContactList(CrmContact contact);

    List<CrmContact> selectCrmContactByCustomerId(Long customerId);

    int insertCrmContact(CrmContact contact);

    int updateCrmContact(CrmContact contact);

    int deleteCrmContactById(Long id);

    int deleteCrmContactByIds(Long[] ids);
}
