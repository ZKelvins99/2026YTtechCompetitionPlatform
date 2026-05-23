package com.ruoyi.system.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.crm.service.ICrmDictService;

/**
 * CRM码表Controller
 */
@RestController
@RequestMapping("/crm/dict")
public class CrmDictController extends BaseController
{
    @Autowired
    private ICrmDictService crmDictService;

    @GetMapping("/{type}")
    public AjaxResult getDict(@PathVariable String type)
    {
        return success(crmDictService.selectDictByType(type));
    }
}
