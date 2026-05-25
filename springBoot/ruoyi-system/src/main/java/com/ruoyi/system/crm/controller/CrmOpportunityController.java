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
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.crm.domain.CrmImportResult;
import com.ruoyi.system.crm.domain.CrmOpportunity;
import com.ruoyi.system.crm.domain.CrmOpportunityImportTemplate;
import com.ruoyi.system.crm.service.ICrmOpportunityService;

/**
 * 商机管理Controller
 */
@RestController
@RequestMapping("/crm/opportunity")
public class CrmOpportunityController extends BaseController
{
    @Autowired
    private ICrmOpportunityService crmOpportunityService;

    @PreAuthorize("@ss.hasPermi('crm:opportunity:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmOpportunity opportunity)
    {
        startPage();
        List<CrmOpportunity> list = crmOpportunityService.selectCrmOpportunityList(opportunity);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('crm:opportunity:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(crmOpportunityService.selectCrmOpportunityById(id));
    }

    @PreAuthorize("@ss.hasPermi('crm:opportunity:add')")
    @Log(title = "商机管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CrmOpportunity opportunity)
    {
        opportunity.setCreateBy(getUsername());
        return toAjax(crmOpportunityService.insertCrmOpportunity(opportunity));
    }

    @PreAuthorize("@ss.hasPermi('crm:opportunity:edit')")
    @Log(title = "商机管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CrmOpportunity opportunity)
    {
        return toAjax(crmOpportunityService.updateCrmOpportunity(opportunity));
    }

    @PreAuthorize("@ss.hasPermi('crm:opportunity:remove')")
    @Log(title = "商机管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(crmOpportunityService.deleteCrmOpportunityByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('crm:opportunity:import')")
    @Log(title = "商机管理", businessType = BusinessType.IMPORT)
    @PostMapping("/import")
    public AjaxResult importData(MultipartFile file) throws Exception
    {
        CrmImportResult result = crmOpportunityService.importOpportunity(file.getInputStream(), getUsername());
        return success(result);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<CrmOpportunityImportTemplate> util = new ExcelUtil<>(CrmOpportunityImportTemplate.class);
        util.importTemplateExcel(response, "商机导入模板");
    }

    @PreAuthorize("@ss.hasPermi('crm:opportunity:import')")
    @Log(title = "商机管理", businessType = BusinessType.INSERT)
    @PostMapping("/seedImportCustomers")
    public AjaxResult seedImportCustomers()
    {
        return AjaxResult.success(crmOpportunityService.seedImportTestCustomers(getUsername()));
    }
}
