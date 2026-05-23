package com.ruoyi.system.crm.domain;

import java.util.List;

/**
 * 工作台布局保存请求
 */
public class CrmWorkbenchLayoutRequest
{
    private List<CrmWorkbenchWidgetItem> widgets;

    public List<CrmWorkbenchWidgetItem> getWidgets() { return widgets; }
    public void setWidgets(List<CrmWorkbenchWidgetItem> widgets) { this.widgets = widgets; }
}
