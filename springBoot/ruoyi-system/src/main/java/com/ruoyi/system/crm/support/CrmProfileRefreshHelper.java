package com.ruoyi.system.crm.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.crm.service.ICrmUserProfileService;

@Component
public class CrmProfileRefreshHelper
{
    @Autowired
    private ICrmUserProfileService crmUserProfileService;

    public void refreshCurrentUser()
    {
        try
        {
            Long userId = SecurityUtils.getUserId();
            if (userId != null)
            {
                crmUserProfileService.refreshAsync(userId);
            }
        }
        catch (Exception ignored)
        {
        }
    }
}
