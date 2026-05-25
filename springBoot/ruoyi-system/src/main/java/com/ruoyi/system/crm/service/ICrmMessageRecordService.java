package com.ruoyi.system.crm.service;

import java.util.List;
import com.ruoyi.system.crm.domain.CrmMessageRecord;
import com.ruoyi.system.crm.domain.CrmMessageSendRequest;

public interface ICrmMessageRecordService
{
    CrmMessageRecord selectCrmMessageRecordById(Long id);

    List<CrmMessageRecord> selectCrmMessageRecordList(CrmMessageRecord record);

    int sendMessage(CrmMessageSendRequest request, Long senderId);

    int recallMessage(Long id, Long operatorId);

    int resendMessage(Long id, Long operatorId);

    int countUnread(Long receiverId);

    List<CrmMessageRecord> selectUnreadList(Long receiverId, int limit);

    List<CrmMessageRecord> selectInboxList(Long userId, int limit);

    int markAsRead(Long id, Long userId);
}
