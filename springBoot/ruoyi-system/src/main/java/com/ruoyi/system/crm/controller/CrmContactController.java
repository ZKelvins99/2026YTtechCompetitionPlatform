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
import com.ruoyi.system.crm.domain.CrmContact;
import com.ruoyi.system.crm.service.ICrmContactService;

/**
 * 联系人管理Controller
 */
@RestController
@RequestMapping("/crm/contact")
public class CrmContactController extends BaseController
{
    @Autowired
    private ICrmContactService crmContactService;

    @PreAuthorize("@ss.hasPermi('crm:contact:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmContact contact)
    {
        startPage();
        List<CrmContact> list = crmContactService.selectCrmContactList(contact);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('crm:contact:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(crmContactService.selectCrmContactById(id));
    }

    @PreAuthorize("@ss.hasPermi('crm:contact:add')")
    @Log(title = "联系人管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CrmContact contact)
    {
        return toAjax(crmContactService.insertCrmContact(contact));
    }

    @PreAuthorize("@ss.hasPermi('crm:contact:edit')")
    @Log(title = "联系人管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CrmContact contact)
    {
        return toAjax(crmContactService.updateCrmContact(contact));
    }

    @PreAuthorize("@ss.hasPermi('crm:contact:remove')")
    @Log(title = "联系人管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(crmContactService.deleteCrmContactByIds(ids));
    }
}
