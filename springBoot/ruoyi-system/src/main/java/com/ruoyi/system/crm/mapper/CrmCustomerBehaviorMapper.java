package com.ruoyi.system.crm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.ResultSetType;
import com.ruoyi.system.crm.domain.CrmCustomerBehavior;

public interface CrmCustomerBehaviorMapper
{
    List<Long> selectNextIds(@Param("count") int count);

    int batchInsert(@Param("list") List<CrmCustomerBehavior> list);

    int deleteAll();

    @Options(fetchSize = 1000, resultSetType = ResultSetType.FORWARD_ONLY)
    List<CrmCustomerBehavior> selectScrollList(@Param("lastId") Long lastId, @Param("pageSize") int pageSize);

    /** 数据字典近似行数，毫秒级（导入后需 DBMS_STATS 才准） */
    long countTotalApprox();

    long countTotal();
}
