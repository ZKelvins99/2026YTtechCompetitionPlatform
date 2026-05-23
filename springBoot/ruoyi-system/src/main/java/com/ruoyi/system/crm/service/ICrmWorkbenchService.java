package com.ruoyi.system.crm.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.crm.domain.CrmWorkbenchLayoutRequest;

public interface ICrmWorkbenchService
{
    Map<String, Object> getLayout(Long userId);

    int saveLayout(Long userId, CrmWorkbenchLayoutRequest request);

    List<Map<String, Object>> getWidgetMetaList();

    Object getWidgetData(String widgetId, Long userId);
}
