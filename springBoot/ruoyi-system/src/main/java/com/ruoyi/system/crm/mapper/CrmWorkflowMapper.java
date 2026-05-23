package com.ruoyi.system.crm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.crm.domain.CrmWorkflowInstance;
import com.ruoyi.system.crm.domain.CrmWorkflowNode;

public interface CrmWorkflowMapper
{
    CrmWorkflowInstance selectInstanceById(Long id);

    CrmWorkflowInstance selectActiveInstanceByBusinessId(@Param("businessId") Long businessId);

    List<CrmWorkflowNode> selectNodesByInstanceId(Long instanceId);

    CrmWorkflowNode selectNodeById(Long id);

    int insertInstance(CrmWorkflowInstance instance);

    int updateInstance(CrmWorkflowInstance instance);

    int insertNode(CrmWorkflowNode node);

    int updateNode(CrmWorkflowNode node);

    int resetNodesFromOrder(@Param("instanceId") Long instanceId, @Param("nodeOrder") Integer nodeOrder);
}
