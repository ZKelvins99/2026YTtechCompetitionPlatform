package com.ruoyi.system.crm.service;

import java.util.Map;

public interface ICrmUserProfileService
{
    Map<String, Object> getProfile(Long userId);

    void refresh(Long userId);

    void refreshAsync(Long userId);
}
