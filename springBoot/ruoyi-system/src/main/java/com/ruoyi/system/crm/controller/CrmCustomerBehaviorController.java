package com.ruoyi.system.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.crm.domain.CrmBehaviorGenerateRequest;
import com.ruoyi.system.crm.domain.CrmBehaviorTaskStatus;
import com.ruoyi.system.crm.service.ICrmCustomerBehaviorService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/crm/behavior")
public class CrmCustomerBehaviorController extends BaseController
{
    @Autowired
    private ICrmCustomerBehaviorService crmCustomerBehaviorService;

    @PreAuthorize("@ss.hasPermi('crm:behavior:import')")
    @Log(title = "客户行为数据", businessType = BusinessType.IMPORT)
    @PostMapping("/import")
    public AjaxResult importData(MultipartFile file) throws Exception
    {
        String taskId = crmCustomerBehaviorService.startImport(file);
        Map<String, String> result = new HashMap<>();
        result.put("taskId", taskId);
        return success(result);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        crmCustomerBehaviorService.writeImportTemplate(response);
    }

    @PreAuthorize("@ss.hasPermi('crm:behavior:import')")
    @GetMapping("/sampleExcel")
    public void sampleExcel(HttpServletResponse response,
                            @RequestParam(defaultValue = "100000") int count) throws Exception
    {
        crmCustomerBehaviorService.writeSampleExcel(response, count);
    }

    @PreAuthorize("@ss.hasPermi('crm:behavior:remove')")
    @Log(title = "客户行为数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/clear")
    public AjaxResult clearAll()
    {
        int rows = crmCustomerBehaviorService.clearAll();
        return success(rows);
    }

    @PreAuthorize("@ss.hasPermi('crm:behavior:generate')")
    @Log(title = "客户行为数据", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public AjaxResult generate(@Validated @RequestBody CrmBehaviorGenerateRequest request)
    {
        String taskId = crmCustomerBehaviorService.startGenerate(request.getCount());
        Map<String, String> result = new HashMap<>();
        result.put("taskId", taskId);
        return success(result);
    }

    @PreAuthorize("@ss.hasPermi('crm:behavior:query')")
    @GetMapping("/task/{taskId}")
    public AjaxResult getTask(@PathVariable String taskId)
    {
        CrmBehaviorTaskStatus task = crmCustomerBehaviorService.getTaskStatus(taskId);
        if (task == null)
        {
            throw new ServiceException("任务不存在或已过期");
        }
        return success(task);
    }

    @PreAuthorize("@ss.hasPermi('crm:behavior:list')")
    @GetMapping("/scroll")
    public AjaxResult scroll(@RequestParam(required = false, defaultValue = "0") Long lastId,
                             @RequestParam(required = false, defaultValue = "100") Integer pageSize,
                             HttpServletResponse response)
    {
        long start = System.currentTimeMillis();
        var data = crmCustomerBehaviorService.scrollList(lastId, pageSize);
        response.setHeader("X-Response-Time", String.valueOf(System.currentTimeMillis() - start));
        return success(data);
    }

    @PreAuthorize("@ss.hasPermi('crm:behavior:list')")
    @GetMapping("/total")
    public AjaxResult total()
    {
        return success(crmCustomerBehaviorService.countTotal());
    }
}
