package com.ruoyi.system.crm.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.crm.domain.CrmMessageRecord;
import com.ruoyi.system.crm.domain.CrmWorkbenchLayout;
import com.ruoyi.system.crm.domain.CrmWorkbenchLayoutRequest;
import com.ruoyi.system.crm.domain.CrmWorkbenchWidgetItem;
import com.ruoyi.system.crm.mapper.CrmMessageRecordMapper;
import com.ruoyi.system.crm.mapper.CrmWorkbenchMapper;
import com.ruoyi.system.crm.service.ICrmWorkbenchService;
import com.ruoyi.system.service.ISysUserService;

@Service
public class CrmWorkbenchServiceImpl implements ICrmWorkbenchService
{
    @Autowired
    private CrmWorkbenchMapper crmWorkbenchMapper;

    @Autowired
    private CrmMessageRecordMapper crmMessageRecordMapper;

    @Autowired
    private ISysUserService sysUserService;

    @Override
    public Map<String, Object> getLayout(Long userId)
    {
        CrmWorkbenchLayout layout = crmWorkbenchMapper.selectByUserId(userId);
        Map<String, Object> result = new HashMap<>();
        if (layout != null && StringUtils.isNotEmpty(layout.getLayoutJson()))
        {
            result.put("widgets", JSON.parseArray(layout.getLayoutJson(), CrmWorkbenchWidgetItem.class));
            result.put("updateTime", layout.getUpdateTime());
        }
        else
        {
            result.put("widgets", defaultWidgets());
            result.put("updateTime", null);
        }
        return result;
    }

    @Override
    public int saveLayout(Long userId, CrmWorkbenchLayoutRequest request)
    {
        CrmWorkbenchLayout layout = new CrmWorkbenchLayout();
        layout.setUserId(userId);
        layout.setLayoutJson(JSON.toJSONString(request.getWidgets()));

        CrmWorkbenchLayout existing = crmWorkbenchMapper.selectByUserId(userId);
        if (existing == null)
        {
            return crmWorkbenchMapper.insertLayout(layout);
        }
        return crmWorkbenchMapper.updateLayout(layout);
    }

    @Override
    public List<Map<String, Object>> getWidgetMetaList()
    {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(meta("approval", "待办审批", "当前用户待审批合同列表"));
        list.add(meta("customer-stats", "客户统计", "负责客户总数与本月新增"));
        list.add(meta("opportunity-funnel", "商机漏斗", "各阶段商机数量分布"));
        list.add(meta("unread-messages", "未读消息", "最近5条未读CRM消息"));
        list.add(meta("quick-actions", "快捷操作", "常用业务快捷入口"));
        list.add(meta("expiring-contracts", "合同到期提醒", "近30天内到期的合同列表"));
        return list;
    }

    @Override
    public Object getWidgetData(String widgetId, Long userId)
    {
        SysUser user = sysUserService.selectUserById(userId);
        String createBy = user != null ? user.getUserName() : "";

        switch (widgetId)
        {
            case "approval":
                return normalizeList(crmWorkbenchMapper.selectPendingApprovals(userId));
            case "customer-stats":
                Map<String, Object> stats = new HashMap<>();
                stats.put("total", crmWorkbenchMapper.countCustomerByCreateBy(createBy));
                stats.put("monthNew", crmWorkbenchMapper.countCustomerThisMonthByCreateBy(createBy));
                return stats;
            case "opportunity-funnel":
                return normalizeList(crmWorkbenchMapper.selectOpportunityStageStats(createBy));
            case "unread-messages":
                List<CrmMessageRecord> messages = crmMessageRecordMapper.selectUnreadList(userId, 5);
                return messages;
            case "quick-actions":
                List<Map<String, Object>> actions = new ArrayList<>();
                actions.add(action("新建客户", "/crm/customer", "crm:customer:add"));
                actions.add(action("新建商机", "/crm/opportunity", "crm:opportunity:add"));
                actions.add(action("发送消息", "/crm/message/send", "crm:message:send"));
                actions.add(action("数据大屏", "/crm/dashboard", "crm:dashboard:view"));
                return actions;
            case "expiring-contracts":
                return normalizeList(crmWorkbenchMapper.selectExpiringContracts());
            default:
                return new HashMap<>();
        }
    }

    private Map<String, Object> meta(String id, String title, String desc)
    {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("title", title);
        m.put("description", desc);
        return m;
    }

    private Map<String, Object> action(String label, String path, String perm)
    {
        Map<String, Object> m = new HashMap<>();
        m.put("label", label);
        m.put("path", path);
        m.put("perm", perm);
        return m;
    }

    private List<CrmWorkbenchWidgetItem> defaultWidgets()
    {
        List<CrmWorkbenchWidgetItem> widgets = new ArrayList<>();
        widgets.add(widget("approval", "待办审批", 0, 0, 6, 4));
        widgets.add(widget("customer-stats", "客户统计", 6, 0, 6, 2));
        widgets.add(widget("opportunity-funnel", "商机漏斗", 0, 4, 6, 4));
        widgets.add(widget("unread-messages", "未读消息", 6, 2, 6, 3));
        widgets.add(widget("expiring-contracts", "合同到期提醒", 6, 5, 6, 3));
        return widgets;
    }

    private CrmWorkbenchWidgetItem widget(String id, String title, int x, int y, int w, int h)
    {
        CrmWorkbenchWidgetItem item = new CrmWorkbenchWidgetItem();
        item.setId(id);
        item.setTitle(title);
        item.setX(x);
        item.setY(y);
        item.setW(w);
        item.setH(h);
        return item;
    }

    private List<Map<String, Object>> normalizeList(List<Map<String, Object>> rows)
    {
        List<Map<String, Object>> result = new ArrayList<>();
        if (rows == null)
        {
            return result;
        }
        for (Map<String, Object> row : rows)
        {
            Map<String, Object> item = new HashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet())
            {
                String key = entry.getKey();
                if (key != null && key.equals(key.toUpperCase()))
                {
                    key = key.toLowerCase();
                }
                item.put(key, entry.getValue());
            }
            result.add(item);
        }
        return result;
    }
}
