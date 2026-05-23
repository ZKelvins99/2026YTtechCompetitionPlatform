package com.ruoyi.system.crm.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
import com.ruoyi.system.crm.domain.CrmMessageRecord;
import com.ruoyi.system.crm.domain.CrmMessageSendRequest;
import com.ruoyi.system.crm.domain.CrmMessageTemplate;
import com.ruoyi.system.crm.service.ICrmMessageRecordService;
import com.ruoyi.system.crm.service.ICrmMessageTemplateService;

@RestController
@RequestMapping("/crm/message/record")
public class CrmMessageRecordController extends BaseController
{
    @Autowired
    private ICrmMessageRecordService crmMessageRecordService;

    @Autowired
    private ICrmMessageTemplateService crmMessageTemplateService;

    @PreAuthorize("@ss.hasPermi('crm:message:record:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmMessageRecord record)
    {
        startPage();
        List<CrmMessageRecord> list = crmMessageRecordService.selectCrmMessageRecordList(record);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('crm:message:send')")
    @GetMapping("/send/template/list")
    public AjaxResult sendTemplateList()
    {
        List<CrmMessageTemplate> list = crmMessageTemplateService.selectActiveTemplateList();
        return success(list);
    }

    @PreAuthorize("@ss.hasPermi('crm:message:send')")
    @Log(title = "消息发送", businessType = BusinessType.INSERT)
    @PostMapping("/send")
    public AjaxResult send(@Validated @RequestBody CrmMessageSendRequest request)
    {
        return toAjax(crmMessageRecordService.sendMessage(request, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('crm:message:recall')")
    @Log(title = "消息撤回", businessType = BusinessType.UPDATE)
    @PutMapping("/recall/{id}")
    public AjaxResult recall(@PathVariable Long id)
    {
        return toAjax(crmMessageRecordService.recallMessage(id, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('crm:message:resend')")
    @Log(title = "消息重发", businessType = BusinessType.UPDATE)
    @PutMapping("/resend/{id}")
    public AjaxResult resend(@PathVariable Long id)
    {
        return toAjax(crmMessageRecordService.resendMessage(id, getUserId()));
    }

    @GetMapping("/unread-count")
    public AjaxResult unreadCount()
    {
        return success(crmMessageRecordService.countUnread(getUserId()));
    }

    @GetMapping("/unread-list")
    public AjaxResult unreadList()
    {
        return success(crmMessageRecordService.selectUnreadList(getUserId(), 5));
    }
}
