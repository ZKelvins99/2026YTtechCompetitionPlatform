package com.ruoyi.system.crm.service;

import java.util.List;
import com.ruoyi.system.crm.domain.CrmContact;

/**
 * 联系人Service接口
 */
public interface ICrmContactService
{
    CrmContact selectCrmContactById(Long id);

    List<CrmContact> selectCrmContactList(CrmContact contact);

    int insertCrmContact(CrmContact contact);

    int updateCrmContact(CrmContact contact);

    int deleteCrmContactByIds(Long[] ids);
}
