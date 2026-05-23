package com.ruoyi.system.crm.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CRM 消息模板 crm_message_template
 */
public class CrmMessageTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称不能超过100个字符")
    private String templateName;

    @NotBlank(message = "模板类型不能为空")
    private String templateType;

    @NotBlank(message = "消息标题不能为空")
    @Size(max = 200, message = "消息标题不能超过200个字符")
    private String title;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    /** 可用变量说明 JSON，如 ["客户名称","合同编号"] */
    private String variables;

    /** 0正常 1停用 */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getVariables() { return variables; }
    public void setVariables(String variables) { this.variables = variables; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
