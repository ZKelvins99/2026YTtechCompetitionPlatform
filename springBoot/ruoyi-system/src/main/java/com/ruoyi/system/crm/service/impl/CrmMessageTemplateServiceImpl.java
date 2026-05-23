package com.ruoyi.system.crm.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.crm.domain.CrmMessageTemplate;
import com.ruoyi.system.crm.mapper.CrmMessageTemplateMapper;
import com.ruoyi.system.crm.service.ICrmMessageTemplateService;

@Service
public class CrmMessageTemplateServiceImpl implements ICrmMessageTemplateService
{
    @Autowired
    private CrmMessageTemplateMapper crmMessageTemplateMapper;

    @Override
    public CrmMessageTemplate selectCrmMessageTemplateById(Long id)
    {
        return crmMessageTemplateMapper.selectCrmMessageTemplateById(id);
    }

    @Override
    public List<CrmMessageTemplate> selectCrmMessageTemplateList(CrmMessageTemplate template)
    {
        return crmMessageTemplateMapper.selectCrmMessageTemplateList(template);
    }

    @Override
    public List<CrmMessageTemplate> selectActiveTemplateList()
    {
        return crmMessageTemplateMapper.selectActiveTemplateList();
    }

    @Override
    public int insertCrmMessageTemplate(CrmMessageTemplate template)
    {
        if (template.getStatus() == null)
        {
            template.setStatus("0");
        }
        return crmMessageTemplateMapper.insertCrmMessageTemplate(template);
    }

    @Override
    public int updateCrmMessageTemplate(CrmMessageTemplate template)
    {
        return crmMessageTemplateMapper.updateCrmMessageTemplate(template);
    }

    @Override
    public int deleteCrmMessageTemplateByIds(Long[] ids)
    {
        return crmMessageTemplateMapper.deleteCrmMessageTemplateByIds(ids);
    }
}
