package com.ruoyi.system.crm.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.crm.domain.CrmContact;
import com.ruoyi.system.crm.mapper.CrmContactMapper;
import com.ruoyi.system.crm.service.ICrmContactService;

/**
 * 联系人Service业务层处理
 */
@Service
public class CrmContactServiceImpl implements ICrmContactService
{
    @Autowired
    private CrmContactMapper crmContactMapper;

    @Override
    public CrmContact selectCrmContactById(Long id)
    {
        return crmContactMapper.selectCrmContactById(id);
    }

    @Override
    public List<CrmContact> selectCrmContactList(CrmContact contact)
    {
        return crmContactMapper.selectCrmContactList(contact);
    }

    @Override
    public int insertCrmContact(CrmContact contact)
    {
        if (contact.getIsPrimary() == null)
        {
            contact.setIsPrimary("0");
        }
        return crmContactMapper.insertCrmContact(contact);
    }

    @Override
    public int updateCrmContact(CrmContact contact)
    {
        return crmContactMapper.updateCrmContact(contact);
    }

    @Override
    public int deleteCrmContactByIds(Long[] ids)
    {
        return crmContactMapper.deleteCrmContactByIds(ids);
    }
}
