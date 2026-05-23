package com.ruoyi.system.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.crm.domain.CrmWorkbenchLayoutRequest;
import com.ruoyi.system.crm.service.ICrmWorkbenchService;

@RestController
@RequestMapping("/crm/workbench")
public class CrmWorkbenchController extends BaseController
{
    @Autowired
    private ICrmWorkbenchService crmWorkbenchService;

    @PreAuthorize("@ss.hasPermi('crm:workbench:view')")
    @GetMapping("/layout")
    public AjaxResult getLayout()
    {
        return success(crmWorkbenchService.getLayout(getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('crm:workbench:save')")
    @Log(title = "工作台布局", businessType = BusinessType.UPDATE)
    @PostMapping("/layout")
    public AjaxResult saveLayout(@RequestBody CrmWorkbenchLayoutRequest request)
    {
        return toAjax(crmWorkbenchService.saveLayout(getUserId(), request));
    }

    @PreAuthorize("@ss.hasPermi('crm:workbench:view')")
    @GetMapping("/widgets")
    public AjaxResult widgets()
    {
        return success(crmWorkbenchService.getWidgetMetaList());
    }

    @PreAuthorize("@ss.hasPermi('crm:workbench:view')")
    @GetMapping("/widget/data/{widgetId}")
    public AjaxResult widgetData(@PathVariable String widgetId)
    {
        return success(crmWorkbenchService.getWidgetData(widgetId, getUserId()));
    }
}
