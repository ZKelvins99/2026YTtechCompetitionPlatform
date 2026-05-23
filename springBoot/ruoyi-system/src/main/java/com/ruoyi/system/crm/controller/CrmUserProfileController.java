package com.ruoyi.system.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.crm.service.ICrmUserProfileService;

@RestController
@RequestMapping("/crm/profile")
public class CrmUserProfileController extends BaseController
{
    @Autowired
    private ICrmUserProfileService crmUserProfileService;

    @PreAuthorize("@ss.hasPermi('crm:profile:view')")
    @GetMapping("/{userId}")
    public AjaxResult getProfile(@PathVariable Long userId)
    {
        return success(crmUserProfileService.getProfile(userId));
    }
}
