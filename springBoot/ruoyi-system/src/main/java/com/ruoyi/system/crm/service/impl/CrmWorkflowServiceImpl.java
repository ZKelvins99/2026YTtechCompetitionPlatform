package com.ruoyi.system.crm.service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.crm.domain.CrmContract;
import com.ruoyi.system.crm.domain.CrmWorkflowActionRequest;
import com.ruoyi.system.crm.domain.CrmWorkflowInstance;
import com.ruoyi.system.crm.domain.CrmWorkflowNode;
import com.ruoyi.system.crm.domain.CrmWorkflowStartRequest;
import com.ruoyi.system.crm.mapper.CrmContractMapper;
import com.ruoyi.system.crm.mapper.CrmWorkflowMapper;
import com.ruoyi.system.crm.service.ICrmWorkflowService;

@Service
public class CrmWorkflowServiceImpl implements ICrmWorkflowService
{
    private static final String[] NODE_NAMES = { "提交", "部门审批", "财务审批", "归档" };
    private static final String[] BPMN_IDS = { "Task_Submit", "Task_Dept", "Task_Finance", "Task_Archive" };

    @Autowired
    private CrmWorkflowMapper workflowMapper;

    @Autowired
    private CrmContractMapper contractMapper;

    @Override
    @Transactional
    public CrmWorkflowInstance startContractApproval(CrmWorkflowStartRequest request, Long userId)
    {
        if (request == null || request.getContractId() == null)
        {
            throw new ServiceException("合同ID不能为空");
        }
        Long contractId = request.getContractId();
        if (request.getDeptApproverId() == null || request.getFinanceApproverId() == null
            || request.getArchiveApproverId() == null)
        {
            throw new ServiceException("请指定部门、财务、归档审批人");
        }

        CrmContract contract = contractMapper.selectCrmContractById(contractId);
        if (contract == null)
        {
            throw new ServiceException("合同不存在");
        }
        if (!"DRAFT".equals(contract.getStatusCode()))
        {
            throw new ServiceException("仅草稿状态合同可发起审批");
        }
        if (workflowMapper.selectActiveInstanceByBusinessId(contractId) != null)
        {
            throw new ServiceException("该合同已有进行中的审批流程");
        }

        CrmWorkflowInstance instance = new CrmWorkflowInstance();
        instance.setProcessType("CONTRACT_APPROVAL");
        instance.setBusinessId(contractId);
        instance.setBusinessNo(contract.getContractNo());
        instance.setStatus("0");
        instance.setStartUserId(userId);
        workflowMapper.insertInstance(instance);

        Long[] approvers = { userId, request.getDeptApproverId(), request.getFinanceApproverId(),
            request.getArchiveApproverId() };
        CrmWorkflowNode deptNode = null;
        for (int i = 0; i < NODE_NAMES.length; i++)
        {
            CrmWorkflowNode node = new CrmWorkflowNode();
            node.setInstanceId(instance.getId());
            node.setNodeName(NODE_NAMES[i]);
            node.setNodeOrder(i + 1);
            node.setApprovalType("SINGLE");
            node.setApproverId(approvers[i]);
            if (i == 0)
            {
                node.setStatus("1");
                node.setOpinion("提交");
                node.setApproveTime(new Date());
            }
            else
            {
                node.setStatus("0");
            }
            workflowMapper.insertNode(node);
            if (i == 1)
            {
                deptNode = node;
            }
        }

        instance.setCurrentNodeId(deptNode.getId());
        workflowMapper.updateInstance(instance);

        Long approvingId = contractMapper.selectStatusIdByCode("APPROVING");
        contractMapper.updateContractStatus(contractId, approvingId);

        return getInstanceDetail(instance.getId(), userId);
    }

    @Override
    public CrmWorkflowInstance getInstanceDetail(Long instanceId, Long userId)
    {
        CrmWorkflowInstance instance = workflowMapper.selectInstanceById(instanceId);
        if (instance == null)
        {
            throw new ServiceException("流程实例不存在");
        }
        List<CrmWorkflowNode> nodes = workflowMapper.selectNodesByInstanceId(instanceId);
        for (CrmWorkflowNode node : nodes)
        {
            if (node.getNodeOrder() != null && node.getNodeOrder() >= 1 && node.getNodeOrder() <= BPMN_IDS.length)
            {
                node.setBpmnElementId(BPMN_IDS[node.getNodeOrder() - 1]);
            }
        }
        instance.setNodes(nodes);
        instance.setCanOperate(resolveCanOperate(instance, userId));
        instance.setBpmnXml(BPMN_TEMPLATE);
        return instance;
    }

    @Override
    public String getBpmnXml(Long instanceId)
    {
        if (workflowMapper.selectInstanceById(instanceId) == null)
        {
            throw new ServiceException("流程实例不存在");
        }
        return BPMN_TEMPLATE;
    }

    @Override
    @Transactional
    public CrmWorkflowInstance approve(CrmWorkflowActionRequest request, Long userId)
    {
        CrmWorkflowInstance instance = loadRunningInstance(request.getInstanceId());
        CrmWorkflowNode current = getCurrentNode(instance);
        assertCanApprove(current, userId);
        fillApproverIfMissing(current, userId);

        current.setStatus("1");
        current.setOpinion(request.getOpinion());
        current.setApproveTime(new Date());
        workflowMapper.updateNode(current);

        if (!isCurrentOrderAllApproved(instance.getId(), current.getNodeOrder()))
        {
            return getInstanceDetail(instance.getId(), userId);
        }

        CrmWorkflowNode next = findPrimaryNodeByOrder(instance.getId(), current.getNodeOrder() + 1);
        if (next != null)
        {
            instance.setCurrentNodeId(next.getId());
            workflowMapper.updateInstance(instance);
        }
        else
        {
            completeInstance(instance);
            contractMapper.updateContractStatus(instance.getBusinessId(),
                contractMapper.selectStatusIdByCode("ACTIVE"));
        }
        return getInstanceDetail(instance.getId(), userId);
    }

    @Override
    @Transactional
    public CrmWorkflowInstance reject(CrmWorkflowActionRequest request, Long userId)
    {
        CrmWorkflowInstance instance = loadRunningInstance(request.getInstanceId());
        CrmWorkflowNode current = getCurrentNode(instance);
        assertCanApprove(current, userId);
        fillApproverIfMissing(current, userId);

        current.setStatus("2");
        current.setOpinion(request.getOpinion());
        current.setApproveTime(new Date());
        workflowMapper.updateNode(current);

        if (current.getNodeOrder() <= 1)
        {
            throw new ServiceException("已是第一个节点，无法驳回");
        }

        CrmWorkflowNode prev = findPrimaryNodeByOrder(instance.getId(), current.getNodeOrder() - 1);
        workflowMapper.resetNodesFromOrder(instance.getId(), prev.getNodeOrder());
        instance.setCurrentNodeId(prev.getId());
        workflowMapper.updateInstance(instance);
        return getInstanceDetail(instance.getId(), userId);
    }

    @Override
    @Transactional
    public CrmWorkflowInstance countersign(CrmWorkflowActionRequest request, Long userId)
    {
        CrmWorkflowInstance instance = loadRunningInstance(request.getInstanceId());
        CrmWorkflowNode current = getCurrentNode(instance);
        assertCanApprove(current, userId);

        if (request.getCountersignUserIds() == null || request.getCountersignUserIds().isEmpty())
        {
            throw new ServiceException("请选择会签审批人");
        }

        for (Long uid : request.getCountersignUserIds())
        {
            CrmWorkflowNode csNode = new CrmWorkflowNode();
            csNode.setInstanceId(instance.getId());
            csNode.setNodeName(current.getNodeName());
            csNode.setNodeOrder(current.getNodeOrder());
            csNode.setApproverId(uid);
            csNode.setApprovalType("COUNTERSIGN");
            csNode.setStatus("0");
            workflowMapper.insertNode(csNode);
        }
        current.setApprovalType("COUNTERSIGN");
        workflowMapper.updateNode(current);
        return getInstanceDetail(instance.getId(), userId);
    }

    @Override
    @Transactional
    public CrmWorkflowInstance transfer(CrmWorkflowActionRequest request, Long userId)
    {
        CrmWorkflowInstance instance = loadRunningInstance(request.getInstanceId());
        CrmWorkflowNode current = getCurrentNode(instance);
        assertCanApprove(current, userId);

        if (request.getApproverId() == null)
        {
            throw new ServiceException("请选择转办人");
        }

        current.setApproverId(request.getApproverId());
        current.setStatus("3");
        current.setOpinion(StringUtils.defaultString(request.getOpinion(), "转办"));
        current.setApproveTime(new Date());
        workflowMapper.updateNode(current);

        CrmWorkflowNode newNode = new CrmWorkflowNode();
        newNode.setInstanceId(instance.getId());
        newNode.setNodeName(current.getNodeName());
        newNode.setNodeOrder(current.getNodeOrder());
        newNode.setApproverId(request.getApproverId());
        newNode.setApprovalType("SINGLE");
        newNode.setStatus("0");
        workflowMapper.insertNode(newNode);
        instance.setCurrentNodeId(newNode.getId());
        workflowMapper.updateInstance(instance);
        return getInstanceDetail(instance.getId(), userId);
    }

    @Override
    @Transactional
    public CrmWorkflowInstance rollback(Long instanceId, Long targetNodeId, String opinion, Long userId)
    {
        CrmWorkflowInstance instance = loadRunningInstance(instanceId);
        CrmWorkflowNode target = workflowMapper.selectNodeById(targetNodeId);
        if (target == null || !target.getInstanceId().equals(instanceId))
        {
            throw new ServiceException("目标节点无效");
        }

        CrmWorkflowNode current = getCurrentNode(instance);
        assertCanApprove(current, userId);
        current.setStatus("4");
        current.setOpinion(opinion);
        current.setApproveTime(new Date());
        workflowMapper.updateNode(current);

        workflowMapper.resetNodesFromOrder(instanceId, target.getNodeOrder());
        instance.setCurrentNodeId(target.getId());
        workflowMapper.updateInstance(instance);
        return getInstanceDetail(instanceId, userId);
    }

    @Override
    @Transactional
    public CrmWorkflowInstance terminate(CrmWorkflowActionRequest request, Long userId)
    {
        CrmWorkflowInstance instance = loadRunningInstance(request.getInstanceId());
        CrmWorkflowNode current = getCurrentNode(instance);
        assertCanApprove(current, userId);
        current.setStatus("2");
        current.setOpinion(StringUtils.defaultString(request.getOpinion(), "流程终止"));
        current.setApproveTime(new Date());
        workflowMapper.updateNode(current);

        instance.setStatus("2");
        instance.setEndTime(new Date());
        workflowMapper.updateInstance(instance);

        contractMapper.updateContractStatus(instance.getBusinessId(),
            contractMapper.selectStatusIdByCode("TERMINATED"));
        return getInstanceDetail(instance.getId(), userId);
    }

    private CrmWorkflowInstance loadRunningInstance(Long instanceId)
    {
        CrmWorkflowInstance instance = workflowMapper.selectInstanceById(instanceId);
        if (instance == null)
        {
            throw new ServiceException("流程实例不存在");
        }
        if (!"0".equals(instance.getStatus()))
        {
            throw new ServiceException("流程已结束，无法操作");
        }
        return instance;
    }

    private CrmWorkflowNode getCurrentNode(CrmWorkflowInstance instance)
    {
        CrmWorkflowNode node = workflowMapper.selectNodeById(instance.getCurrentNodeId());
        if (node == null)
        {
            throw new ServiceException("当前节点不存在");
        }
        return node;
    }

    private void assertCanApprove(CrmWorkflowNode node, Long userId)
    {
        if (!"0".equals(node.getStatus()))
        {
            throw new ServiceException("当前节点不可操作");
        }
        if (SecurityUtils.isAdmin(userId))
        {
            return;
        }
        if (node.getApproverId() == null || !node.getApproverId().equals(userId))
        {
            throw new ServiceException("您不是当前节点审批人，无权操作");
        }
    }

    private void fillApproverIfMissing(CrmWorkflowNode node, Long userId)
    {
        if (node.getApproverId() == null && userId != null)
        {
            node.setApproverId(userId);
        }
    }

    private boolean resolveCanOperate(CrmWorkflowInstance instance, Long userId)
    {
        if (!"0".equals(instance.getStatus()) || instance.getCurrentNodeId() == null)
        {
            return false;
        }
        CrmWorkflowNode current = workflowMapper.selectNodeById(instance.getCurrentNodeId());
        if (current == null || !"0".equals(current.getStatus()))
        {
            return false;
        }
        if (SecurityUtils.isAdmin(userId))
        {
            return true;
        }
        return userId != null && userId.equals(current.getApproverId());
    }

    private boolean isCurrentOrderAllApproved(Long instanceId, int nodeOrder)
    {
        List<CrmWorkflowNode> nodes = workflowMapper.selectNodesByInstanceId(instanceId);
        List<CrmWorkflowNode> sameOrder = nodes.stream()
            .filter(n -> n.getNodeOrder() == nodeOrder)
            .collect(Collectors.toList());
        return sameOrder.stream().allMatch(n -> "1".equals(n.getStatus()));
    }

    private CrmWorkflowNode findPrimaryNodeByOrder(Long instanceId, int order)
    {
        return workflowMapper.selectNodesByInstanceId(instanceId).stream()
            .filter(n -> n.getNodeOrder() == order && "SINGLE".equals(n.getApprovalType()))
            .findFirst().orElse(null);
    }

    private void completeInstance(CrmWorkflowInstance instance)
    {
        instance.setStatus("1");
        instance.setEndTime(new Date());
        instance.setCurrentNodeId(null);
        workflowMapper.updateInstance(instance);
    }

    private static final String BPMN_TEMPLATE = loadBpmnTemplate();

    private static String loadBpmnTemplate()
    {
        try
        {
            ClassPathResource resource = new ClassPathResource("bpmn/contract-approval.bpmn");
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("无法加载合同审批 BPMN 模板", e);
        }
    }
}
