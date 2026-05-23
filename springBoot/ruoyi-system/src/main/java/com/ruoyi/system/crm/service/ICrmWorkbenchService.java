package com.ruoyi.system.crm.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.crm.domain.CrmWorkbenchLayoutRequest;

/**
 * 工作台服务接口
 * <p>
 * 管理用户个性化工作台布局（JSON 持久化），提供小组件元数据列表
 * 及各组件业务数据的实时获取。</p>
 */
public interface ICrmWorkbenchService
{
    /**
     * 获取用户的工作台布局
     * @param userId 用户 ID
     * @return map 包含 widgets（组件位置/尺寸列表）和 updateTime
     */
    Map<String, Object> getLayout(Long userId);

    /**
     * 保存用户的工作台布局
     * @param userId 用户 ID
     * @param request 布局请求体（widgets 数组）
     * @return 影响行数
     */
    int saveLayout(Long userId, CrmWorkbenchLayoutRequest request);

    /**
     * 获取所有可选小组件的元数据列表
     * @return 列表，每个元素包含 id / title / description
     */
    List<Map<String, Object>> getWidgetMetaList();

    /**
     * 获取指定小组件的业务数据
     * @param widgetId 组件标识（approval / customer-stats / opportunity-funnel 等）
     * @param userId 当前用户 ID
     * @return 组件数据（结构因组件而异）
     */
    Object getWidgetData(String widgetId, Long userId);
}
