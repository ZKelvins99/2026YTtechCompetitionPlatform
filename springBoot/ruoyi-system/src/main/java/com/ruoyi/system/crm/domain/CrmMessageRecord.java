package com.ruoyi.system.crm.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * CRM 消息记录 crm_message_record
 * status: 0已发送 1已撤回 2已读
 */
public class CrmMessageRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long templateId;
    private Long senderId;
    private Long receiverId;
    private String title;
    private String content;
    /** 0已发送 1已撤回 2已读 */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date sendTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date recallTime;

    /** 查询条件：当前用户须为发送人或接收人 */
    private Long participantId;

    /** 展示字段 */
    private String senderName;
    private String receiverName;
    private String templateName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getSendTime() { return sendTime; }
    public void setSendTime(Date sendTime) { this.sendTime = sendTime; }
    public Date getRecallTime() { return recallTime; }
    public void setRecallTime(Date recallTime) { this.recallTime = recallTime; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public Long getParticipantId() { return participantId; }
    public void setParticipantId(Long participantId) { this.participantId = participantId; }
}
