package com.ruoyi.system.crm.domain;

/**
 * 客户行为模块常量（避免 ServiceImpl 与 AsyncService 循环依赖）
 */
public final class CrmCustomerBehaviorConstants
{
    public static final String[] BEHAVIOR_TYPES = { "电话沟通", "拜访", "邮件", "演示" };

    private CrmCustomerBehaviorConstants()
    {
    }
}
