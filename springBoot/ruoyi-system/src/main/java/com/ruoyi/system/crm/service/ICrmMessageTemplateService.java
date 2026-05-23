package com.ruoyi.system.crm.service;

import java.util.List;
import com.ruoyi.system.crm.domain.CrmMessageTemplate;

public interface ICrmMessageTemplateService
{
    CrmMessageTemplate selectCrmMessageTemplateById(Long id);

    List<CrmMessageTemplate> selectCrmMessageTemplateList(CrmMessageTemplate template);

    List<CrmMessageTemplate> selectActiveTemplateList();

    int insertCrmMessageTemplate(CrmMessageTemplate template);

    int updateCrmMessageTemplate(CrmMessageTemplate template);

    int deleteCrmMessageTemplateByIds(Long[] ids);
}
