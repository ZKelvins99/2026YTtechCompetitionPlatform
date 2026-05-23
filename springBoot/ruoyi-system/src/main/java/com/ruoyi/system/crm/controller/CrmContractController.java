package com.ruoyi.system.crm.controller;

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
import com.ruoyi.system.crm.domain.CrmContract;
import com.ruoyi.system.crm.service.ICrmContractService;

@RestController
@RequestMapping("/crm/contract")
public class CrmContractController extends BaseController
{
    @Autowired
    private ICrmContractService crmContractService;

    @PreAuthorize("@ss.hasPermi('crm:contract:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmContract contract)
    {
        startPage();
        List<CrmContract> list = crmContractService.selectCrmContractList(contract);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('crm:contract:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(crmContractService.selectCrmContractById(id));
    }

    @PreAuthorize("@ss.hasPermi('crm:contract:add')")
    @Log(title = "合同管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CrmContract contract)
    {
        contract.setCreateBy(getUsername());
        return toAjax(crmContractService.insertCrmContract(contract));
    }

    @PreAuthorize("@ss.hasPermi('crm:contract:edit')")
    @Log(title = "合同管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CrmContract contract)
    {
        return toAjax(crmContractService.updateCrmContract(contract));
    }

    @PreAuthorize("@ss.hasPermi('crm:contract:remove')")
    @Log(title = "合同管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(crmContractService.deleteCrmContractByIds(ids));
    }
}
