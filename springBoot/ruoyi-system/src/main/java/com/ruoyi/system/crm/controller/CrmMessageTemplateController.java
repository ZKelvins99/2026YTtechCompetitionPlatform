package com.ruoyi.system.crm.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.crm.domain.CrmMessageTemplate;
import com.ruoyi.system.crm.service.ICrmMessageTemplateService;

@RestController
@RequestMapping("/crm/message/template")
public class CrmMessageTemplateController extends BaseController
{
    @Autowired
    private ICrmMessageTemplateService crmMessageTemplateService;

    @PreAuthorize("@ss.hasPermi('crm:message:template:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmMessageTemplate template)
    {
        startPage();
        List<CrmMessageTemplate> list = crmMessageTemplateService.selectCrmMessageTemplateList(template);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('crm:message:template:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(crmMessageTemplateService.selectCrmMessageTemplateById(id));
    }

    @PreAuthorize("@ss.hasPermi('crm:message:template:add')")
    @Log(title = "消息模板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CrmMessageTemplate template)
    {
        return toAjax(crmMessageTemplateService.insertCrmMessageTemplate(template));
    }

    @PreAuthorize("@ss.hasPermi('crm:message:template:edit')")
    @Log(title = "消息模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CrmMessageTemplate template)
    {
        return toAjax(crmMessageTemplateService.updateCrmMessageTemplate(template));
    }

    @PreAuthorize("@ss.hasPermi('crm:message:template:remove')")
    @Log(title = "消息模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(crmMessageTemplateService.deleteCrmMessageTemplateByIds(ids));
    }
}
