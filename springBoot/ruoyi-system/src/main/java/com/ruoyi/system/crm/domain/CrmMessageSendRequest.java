package com.ruoyi.system.crm.domain;

import java.util.Map;
import jakarta.validation.constraints.NotNull;

/**
 * 引用模板发送消息请求
 */
public class CrmMessageSendRequest
{
    @NotNull(message = "模板不能为空")
    private Long templateId;

    @NotNull(message = "接收人不能为空")
    private Long receiverId;

    /** 变量名 -> 变量值 */
    private Map<String, String> variables;

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }
}
