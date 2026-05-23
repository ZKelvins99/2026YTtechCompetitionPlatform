package com.ruoyi.system.crm.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.ruoyi.system.crm.domain.CrmApiDebugRequest;
import com.ruoyi.system.crm.domain.CrmApiInfo;
import com.ruoyi.system.crm.service.ICrmApiInfoService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/crm/api")
public class CrmApiInfoController extends BaseController
{
    @Autowired
    private ICrmApiInfoService crmApiInfoService;

    @PreAuthorize("@ss.hasPermi('crm:api:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmApiInfo apiInfo)
    {
        startPage();
        List<CrmApiInfo> list = crmApiInfoService.selectCrmApiInfoList(apiInfo);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('crm:api:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(crmApiInfoService.selectCrmApiInfoById(id));
    }

    @PreAuthorize("@ss.hasPermi('crm:api:add')")
    @Log(title = "API服务市场", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CrmApiInfo apiInfo)
    {
        apiInfo.setCreateBy(getUsername());
        return toAjax(crmApiInfoService.insertCrmApiInfo(apiInfo));
    }

    @PreAuthorize("@ss.hasPermi('crm:api:edit')")
    @Log(title = "API服务市场", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CrmApiInfo apiInfo)
    {
        return toAjax(crmApiInfoService.updateCrmApiInfo(apiInfo));
    }

    @PreAuthorize("@ss.hasPermi('crm:api:remove')")
    @Log(title = "API服务市场", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(crmApiInfoService.deleteCrmApiInfoByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('crm:api:online')")
    @Log(title = "API上架", businessType = BusinessType.UPDATE)
    @PutMapping("/online/{id}")
    public AjaxResult online(@PathVariable Long id)
    {
        return toAjax(crmApiInfoService.online(id));
    }

    @PreAuthorize("@ss.hasPermi('crm:api:online')")
    @Log(title = "API下架", businessType = BusinessType.UPDATE)
    @PutMapping("/offline/{id}")
    public AjaxResult offline(@PathVariable Long id)
    {
        return toAjax(crmApiInfoService.offline(id));
    }

    @PreAuthorize("@ss.hasPermi('crm:api:debug')")
    @PostMapping("/debug/{id}")
    public AjaxResult debug(@PathVariable Long id, @RequestBody(required = false) CrmApiDebugRequest request,
                            HttpServletRequest httpRequest)
    {
        return success(crmApiInfoService.debugApi(id, request, httpRequest.getHeader("Authorization")));
    }

    @GetMapping("/demo/customer-count")
    public AjaxResult customerCount()
    {
        Map<String, Object> data = new HashMap<>();
        data.put("count", crmApiInfoService.getCustomerCount());
        return success(data);
    }
}
