package com.ruoyi.system.crm.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.crm.domain.CrmOperLog;
import com.ruoyi.system.crm.service.ICrmOperLogService;

@RestController
@RequestMapping("/crm/operlog")
public class CrmOperLogController extends BaseController
{
    @Autowired
    private ICrmOperLogService crmOperLogService;

    @PreAuthorize("@ss.hasPermi('crm:operlog:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmOperLog operLog)
    {
        startPage();
        List<CrmOperLog> list = crmOperLogService.selectCrmOperLogList(operLog);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('crm:operlog:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(crmOperLogService.selectCrmOperLogById(id));
    }
}
