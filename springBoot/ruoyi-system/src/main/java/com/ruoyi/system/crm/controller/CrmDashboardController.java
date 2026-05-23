package com.ruoyi.system.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.crm.service.ICrmDashboardService;

@RestController
@RequestMapping("/crm/dashboard")
public class CrmDashboardController extends BaseController
{
    @Autowired
    private ICrmDashboardService crmDashboardService;

    @PreAuthorize("@ss.hasPermi('crm:dashboard:view')")
    @GetMapping("/system-status")
    public AjaxResult systemStatus() throws Exception
    {
        return success(crmDashboardService.getSystemStatus());
    }

    @PreAuthorize("@ss.hasPermi('crm:dashboard:view')")
    @GetMapping("/log-stats")
    public AjaxResult logStats()
    {
        return success(crmDashboardService.getLogStats());
    }
}
