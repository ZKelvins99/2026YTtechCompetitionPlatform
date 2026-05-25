package com.ruoyi.system.crm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.crm.domain.CrmApiInfo;

public interface CrmApiInfoMapper
{
    CrmApiInfo selectCrmApiInfoById(Long id);

    CrmApiInfo selectCrmApiInfoByUrlAndMethod(@Param("apiUrl") String apiUrl, @Param("apiMethod") String apiMethod);

    List<CrmApiInfo> selectCrmApiInfoList(CrmApiInfo apiInfo);

    int insertCrmApiInfo(CrmApiInfo apiInfo);

    int updateCrmApiInfo(CrmApiInfo apiInfo);

    int deleteCrmApiInfoByIds(Long[] ids);

    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
