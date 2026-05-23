package com.ruoyi.system.crm.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
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
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.crm.domain.CrmCustomer;
import com.ruoyi.system.crm.service.ICrmCustomerService;

/**
 * 客户管理Controller
 */
@RestController
@RequestMapping("/crm/customer")
public class CrmCustomerController extends BaseController
{
    @Autowired
    private ICrmCustomerService crmCustomerService;

    @PreAuthorize("@ss.hasPermi('crm:customer:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmCustomer customer)
    {
        startPage();
        List<CrmCustomer> list = crmCustomerService.selectCrmCustomerList(customer);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('crm:customer:export')")
    @Log(title = "客户管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CrmCustomer customer)
    {
        List<CrmCustomer> list = crmCustomerService.selectCrmCustomerList(customer);
        ExcelUtil<CrmCustomer> util = new ExcelUtil<>(CrmCustomer.class);
        util.exportExcel(response, list, "客户数据");
    }

    @PreAuthorize("@ss.hasPermi('crm:customer:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(crmCustomerService.selectCrmCustomerById(id));
    }

    @PreAuthorize("@ss.hasPermi('crm:customer:add')")
    @Log(title = "客户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CrmCustomer customer)
    {
        customer.setCreateBy(getUsername());
        return toAjax(crmCustomerService.insertCrmCustomer(customer));
    }

    @PreAuthorize("@ss.hasPermi('crm:customer:edit')")
    @Log(title = "客户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CrmCustomer customer)
    {
        customer.setUpdateBy(getUsername());
        return toAjax(crmCustomerService.updateCrmCustomer(customer));
    }

    @PreAuthorize("@ss.hasPermi('crm:customer:remove')")
    @Log(title = "客户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(crmCustomerService.deleteCrmCustomerByIds(ids));
    }
}
