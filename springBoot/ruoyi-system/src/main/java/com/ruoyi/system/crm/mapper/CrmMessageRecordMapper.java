package com.ruoyi.system.crm.mapper;

import java.util.List;
import com.ruoyi.system.crm.domain.CrmMessageRecord;
import org.apache.ibatis.annotations.Param;

public interface CrmMessageRecordMapper
{
    CrmMessageRecord selectCrmMessageRecordById(Long id);

    List<CrmMessageRecord> selectCrmMessageRecordList(CrmMessageRecord record);

    int insertCrmMessageRecord(CrmMessageRecord record);

    int updateCrmMessageRecord(CrmMessageRecord record);

    int countUnreadByReceiver(@Param("receiverId") Long receiverId);

    List<CrmMessageRecord> selectUnreadList(@Param("receiverId") Long receiverId, @Param("limit") int limit);

    List<CrmMessageRecord> selectInboxList(@Param("userId") Long userId, @Param("limit") int limit);

    int markAsRead(@Param("id") Long id, @Param("receiverId") Long receiverId);
}
