package com.ruoyi.system.crm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.crm.domain.CrmCustomerBehavior;

public interface CrmCustomerBehaviorMapper
{
    int batchInsert(@Param("list") List<CrmCustomerBehavior> list);

    List<CrmCustomerBehavior> selectScrollList(@Param("lastId") Long lastId, @Param("pageSize") int pageSize);

    long countTotal();
}
