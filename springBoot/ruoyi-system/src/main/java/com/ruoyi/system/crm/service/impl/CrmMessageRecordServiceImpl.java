package com.ruoyi.system.crm.service.impl;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.crm.domain.CrmMessageRecord;
import com.ruoyi.system.crm.domain.CrmMessageSendRequest;
import com.ruoyi.system.crm.domain.CrmMessageTemplate;
import com.ruoyi.system.crm.mapper.CrmMessageRecordMapper;
import com.ruoyi.system.crm.mapper.CrmMessageTemplateMapper;
import com.ruoyi.system.crm.service.ICrmMessageRecordService;

@Service
public class CrmMessageRecordServiceImpl implements ICrmMessageRecordService
{
    @Autowired
    private CrmMessageRecordMapper crmMessageRecordMapper;

    @Autowired
    private CrmMessageTemplateMapper crmMessageTemplateMapper;

    @Override
    public CrmMessageRecord selectCrmMessageRecordById(Long id)
    {
        return crmMessageRecordMapper.selectCrmMessageRecordById(id);
    }

    @Override
    public List<CrmMessageRecord> selectCrmMessageRecordList(CrmMessageRecord record)
    {
        return crmMessageRecordMapper.selectCrmMessageRecordList(record);
    }

    @Override
    public int sendMessage(CrmMessageSendRequest request, Long senderId)
    {
        if (senderId == null || request.getReceiverId() == null)
        {
            throw new ServiceException("发送人或接收人不能为空");
        }
        if (senderId.equals(request.getReceiverId()))
        {
            throw new ServiceException("发送人与接收人不能为同一人");
        }

        CrmMessageTemplate template = crmMessageTemplateMapper.selectCrmMessageTemplateById(request.getTemplateId());
        if (template == null)
        {
            throw new ServiceException("消息模板不存在");
        }
        if (!"0".equals(template.getStatus()))
        {
            throw new ServiceException("消息模板已停用");
        }

        CrmMessageRecord record = new CrmMessageRecord();
        record.setTemplateId(template.getId());
        record.setSenderId(senderId);
        record.setReceiverId(request.getReceiverId());
        record.setTitle(renderText(template.getTitle(), request.getVariables()));
        record.setContent(renderText(template.getContent(), request.getVariables()));
        record.setStatus("0");
        return crmMessageRecordMapper.insertCrmMessageRecord(record);
    }

    @Override
    public int recallMessage(Long id, Long operatorId)
    {
        CrmMessageRecord record = requireParticipantRecord(id, operatorId);

        if (!"0".equals(record.getStatus()))
        {
            throw new ServiceException("仅已发送状态的消息可撤回");
        }
        if (!operatorId.equals(record.getSenderId()))
        {
            throw new ServiceException("仅发送人可撤回消息");
        }
        CrmMessageRecord update = new CrmMessageRecord();
        update.setId(id);
        update.setStatus("1");
        update.setRecallTime(new java.util.Date());
        return crmMessageRecordMapper.updateCrmMessageRecord(update);
    }

    @Override
    public int resendMessage(Long id, Long operatorId)
    {
        CrmMessageRecord record = requireParticipantRecord(id, operatorId);

        if (!"1".equals(record.getStatus()))
        {
            throw new ServiceException("仅已撤回状态的消息可重发");
        }
        if (!operatorId.equals(record.getSenderId()))
        {
            throw new ServiceException("仅发送人可重发消息");
        }
        CrmMessageRecord update = new CrmMessageRecord();
        update.setId(id);
        update.setStatus("0");
        update.setRecallTime(null);
        update.setSendTime(new java.util.Date());
        return crmMessageRecordMapper.updateCrmMessageRecord(update);
    }

    @Override
    public int markAsRead(Long id, Long userId)
    {
        CrmMessageRecord record = requireParticipantRecord(id, userId);
        if (!userId.equals(record.getReceiverId()))
        {
            throw new ServiceException("仅接收人可标记已读");
        }
        if (!"0".equals(record.getStatus()))
        {
            throw new ServiceException("仅未读消息可标记已读");
        }
        return crmMessageRecordMapper.markAsRead(id, userId);
    }

    @Override
    public int countUnread(Long receiverId)
    {
        return crmMessageRecordMapper.countUnreadByReceiver(receiverId);
    }

    @Override
    public List<CrmMessageRecord> selectUnreadList(Long receiverId, int limit)
    {
        return crmMessageRecordMapper.selectUnreadList(receiverId, limit);
    }

    @Override
    public List<CrmMessageRecord> selectInboxList(Long userId, int limit)
    {
        return crmMessageRecordMapper.selectInboxList(userId, limit);
    }

    private CrmMessageRecord requireParticipantRecord(Long id, Long userId)
    {
        CrmMessageRecord record = crmMessageRecordMapper.selectCrmMessageRecordById(id);
        if (record == null)
        {
            throw new ServiceException("消息记录不存在");
        }
        if (userId == null || (!userId.equals(record.getSenderId()) && !userId.equals(record.getReceiverId())))
        {
            throw new ServiceException("无权操作该消息");
        }
        return record;
    }

    private String renderText(String text, Map<String, String> variables)
    {
        if (StringUtils.isEmpty(text) || variables == null || variables.isEmpty())
        {
            return text;
        }
        String result = text;
        for (Map.Entry<String, String> entry : variables.entrySet())
        {
            String key = entry.getKey();
            String val = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace("{" + key + "}", val);
        }
        return result;
    }
}
