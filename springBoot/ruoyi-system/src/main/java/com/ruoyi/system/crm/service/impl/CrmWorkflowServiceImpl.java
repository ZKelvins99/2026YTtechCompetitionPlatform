package com.ruoyi.system.crm.service.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.model.LoginUser;
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
                insertCountersignNodes(instance.getId(), node.getNodeOrder(), node.getNodeName(),
                    request.getDeptCountersignIds(), request.getDeptApproverId());
            }
            else if (i == 2)
            {
                insertCountersignNodes(instance.getId(), node.getNodeOrder(), node.getNodeName(),
                    request.getFinanceCountersignIds(), request.getFinanceApproverId());
            }
            else if (i == 3)
            {
                insertCountersignNodes(instance.getId(), node.getNodeOrder(), node.getNodeName(),
                    request.getArchiveCountersignIds(), request.getArchiveApproverId());
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
        syncCurrentNodePointer(instance, nodes);
        enrichStageSummary(instance, nodes, userId);
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
        CrmWorkflowNode current = resolveOperatingNode(instance, userId);
        assertCanApprove(current, userId);
        fillApproverIfMissing(current, userId);

        current.setStatus("1");
        current.setOpinion(request.getOpinion());
        current.setApproveTime(new Date());
        workflowMapper.updateNode(current);

        if (!isCurrentOrderAllApproved(instance.getId(), current.getNodeOrder()))
        {
            List<CrmWorkflowNode> nodes = workflowMapper.selectNodesByInstanceId(instance.getId());
            syncCurrentNodePointer(instance, nodes);
            workflowMapper.updateInstance(instance);
            return getInstanceDetail(instance.getId(), userId);
        }

        advanceToNextOrder(instance, current.getNodeOrder());
        return getInstanceDetail(instance.getId(), userId);
    }

    @Override
    @Transactional
    public CrmWorkflowInstance reject(CrmWorkflowActionRequest request, Long userId)
    {
        CrmWorkflowInstance instance = loadRunningInstance(request.getInstanceId());
        CrmWorkflowNode current = resolveOperatingNode(instance, userId);
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
        CrmWorkflowNode current = resolveOperatingNode(instance, userId);
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
        CrmWorkflowNode current = resolveOperatingNode(instance, userId);
        assertCanApprove(current, userId);

        if (request.getApproverId() == null)
        {
            throw new ServiceException("请选择转办人");
        }

        current.setApproverId(request.getApproverId());
        current.setStatus("3");
        current.setOpinion(StringUtils.isNotEmpty(request.getOpinion()) ? request.getOpinion() : "转办");
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

        List<CrmWorkflowNode> nodes = workflowMapper.selectNodesByInstanceId(instanceId);
        Integer activeOrder = resolveActiveOrder(nodes);
        List<CrmWorkflowNode> allowedTargets = resolveRollbackTargets(nodes, activeOrder);
        if (allowedTargets.isEmpty())
        {
            throw new ServiceException("当前环节之前无已通过的审批节点，无法回退");
        }
        boolean targetAllowed = allowedTargets.stream().anyMatch(n -> n.getId().equals(targetNodeId));
        if (!targetAllowed)
        {
            throw new ServiceException("只能回退到已审批通过且早于当前环节的节点");
        }
        if (!"SINGLE".equals(target.getApprovalType()))
        {
            throw new ServiceException("只能回退到主审节点");
        }
        if (target.getNodeOrder() == null || activeOrder == null || target.getNodeOrder() >= activeOrder)
        {
            throw new ServiceException("不能回退到当前或之后的环节");
        }

        CrmWorkflowNode current = resolveOperatingNode(instance, userId);
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
        CrmWorkflowNode current = resolveOperatingNode(instance, userId);
        assertCanApprove(current, userId);
        current.setStatus("2");
        current.setOpinion(StringUtils.isNotEmpty(request.getOpinion()) ? request.getOpinion() : "流程终止");
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

    private CrmWorkflowNode resolveOperatingNode(CrmWorkflowInstance instance, Long userId)
    {
        List<CrmWorkflowNode> nodes = workflowMapper.selectNodesByInstanceId(instance.getId());
        Integer activeOrder = resolveActiveOrder(nodes);
        if (activeOrder == null)
        {
            throw new ServiceException("当前无待办节点");
        }
        List<CrmWorkflowNode> pending = pendingAtOrder(nodes, activeOrder);
        if (pending.isEmpty())
        {
            throw new ServiceException("当前环节已无待办");
        }
        if (isWorkflowPrivileged(userId))
        {
            if (instance.getCurrentNodeId() != null)
            {
                for (CrmWorkflowNode n : pending)
                {
                    if (n.getId().equals(instance.getCurrentNodeId()))
                    {
                        return n;
                    }
                }
            }
            return pending.get(0);
        }
        for (CrmWorkflowNode n : pending)
        {
            if (userId != null && userId.equals(n.getApproverId()))
            {
                return n;
            }
        }
        throw new ServiceException("您不是当前环节审批人，无权操作");
    }

    private void assertCanApprove(CrmWorkflowNode node, Long userId)
    {
        if (!"0".equals(node.getStatus()))
        {
            throw new ServiceException("当前节点不可操作");
        }
        if (isWorkflowPrivileged(userId))
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

    private void enrichStageSummary(CrmWorkflowInstance instance, List<CrmWorkflowNode> nodes, Long userId)
    {
        instance.setCanOperate(resolveCanOperate(instance, nodes, userId));
        Integer activeOrder = resolveActiveOrder(nodes);
        instance.setActiveNodeOrder(activeOrder);
        if (activeOrder != null && activeOrder >= 1 && activeOrder <= NODE_NAMES.length)
        {
            instance.setCurrentStageName(NODE_NAMES[activeOrder - 1]);
        }
        instance.setPendingHint(buildPendingHint(nodes, activeOrder));
        List<CrmWorkflowNode> rollbackTargets = resolveRollbackTargets(nodes, activeOrder);
        instance.setRollbackTargets(rollbackTargets);
        instance.setCanRollback(instance.getCanOperate() != null && instance.getCanOperate() && !rollbackTargets.isEmpty());
    }

    private boolean resolveCanOperate(CrmWorkflowInstance instance, List<CrmWorkflowNode> nodes, Long userId)
    {
        if (!"0".equals(instance.getStatus()))
        {
            return false;
        }
        Integer activeOrder = resolveActiveOrder(nodes);
        if (activeOrder == null)
        {
            return false;
        }
        List<CrmWorkflowNode> pending = pendingAtOrder(nodes, activeOrder);
        if (pending.isEmpty())
        {
            return false;
        }
        if (isWorkflowPrivileged(userId))
        {
            return true;
        }
        return pending.stream().anyMatch(n -> userId != null && userId.equals(n.getApproverId()));
    }

    private void syncCurrentNodePointer(CrmWorkflowInstance instance, List<CrmWorkflowNode> nodes)
    {
        if (!"0".equals(instance.getStatus()))
        {
            return;
        }
        Integer activeOrder = resolveActiveOrder(nodes);
        if (activeOrder == null)
        {
            return;
        }
        List<CrmWorkflowNode> pending = pendingAtOrder(nodes, activeOrder);
        if (pending.isEmpty())
        {
            return;
        }
        Long pointer = instance.getCurrentNodeId();
        boolean pointerValid = pointer != null && pending.stream().anyMatch(n -> n.getId().equals(pointer));
        if (!pointerValid)
        {
            instance.setCurrentNodeId(pending.get(0).getId());
            workflowMapper.updateInstance(instance);
        }
    }

    private void advanceToNextOrder(CrmWorkflowInstance instance, int completedOrder)
    {
        List<CrmWorkflowNode> nodes = workflowMapper.selectNodesByInstanceId(instance.getId());
        int nextOrder = completedOrder + 1;
        List<CrmWorkflowNode> nextPending = nodes.stream()
            .filter(n -> n.getNodeOrder() != null && n.getNodeOrder() == nextOrder && "0".equals(n.getStatus()))
            .collect(Collectors.toList());
        if (!nextPending.isEmpty())
        {
            instance.setCurrentNodeId(nextPending.get(0).getId());
            workflowMapper.updateInstance(instance);
            return;
        }
        if (nextOrder <= NODE_NAMES.length)
        {
            CrmWorkflowNode next = findPrimaryNodeByOrder(instance.getId(), nextOrder);
            if (next != null)
            {
                instance.setCurrentNodeId(next.getId());
                workflowMapper.updateInstance(instance);
                return;
            }
        }
        completeInstance(instance);
        contractMapper.updateContractStatus(instance.getBusinessId(),
            contractMapper.selectStatusIdByCode("ACTIVE"));
    }

    private Integer resolveActiveOrder(List<CrmWorkflowNode> nodes)
    {
        return nodes.stream()
            .filter(n -> "0".equals(n.getStatus()) && n.getNodeOrder() != null && n.getNodeOrder() > 1)
            .map(CrmWorkflowNode::getNodeOrder)
            .min(Integer::compareTo)
            .orElse(null);
    }

    private List<CrmWorkflowNode> pendingAtOrder(List<CrmWorkflowNode> nodes, int order)
    {
        return nodes.stream()
            .filter(n -> n.getNodeOrder() != null && n.getNodeOrder() == order && "0".equals(n.getStatus()))
            .collect(Collectors.toList());
    }

    private String buildPendingHint(List<CrmWorkflowNode> nodes, Integer activeOrder)
    {
        if (activeOrder == null)
        {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (CrmWorkflowNode n : pendingAtOrder(nodes, activeOrder))
        {
            String name = StringUtils.isNotEmpty(n.getApproverName()) ? n.getApproverName() : "未指定";
            if ("COUNTERSIGN".equals(n.getApprovalType()))
            {
                parts.add(name + "（会签）");
            }
            else
            {
                parts.add(name + "（主审）");
            }
        }
        return parts.isEmpty() ? null : String.join("、", parts);
    }

    private void insertCountersignNodes(Long instanceId, int nodeOrder, String nodeName,
        List<Long> userIds, Long primaryApproverId)
    {
        if (userIds == null || userIds.isEmpty())
        {
            return;
        }
        Set<Long> added = new HashSet<>();
        for (Long uid : userIds)
        {
            if (uid == null || uid.equals(primaryApproverId) || !added.add(uid))
            {
                continue;
            }
            CrmWorkflowNode csNode = new CrmWorkflowNode();
            csNode.setInstanceId(instanceId);
            csNode.setNodeName(nodeName);
            csNode.setNodeOrder(nodeOrder);
            csNode.setApproverId(uid);
            csNode.setApprovalType("COUNTERSIGN");
            csNode.setStatus("0");
            workflowMapper.insertNode(csNode);
        }
    }

    private boolean isWorkflowPrivileged(Long userId)
    {
        if (SecurityUtils.isAdmin(userId))
        {
            return true;
        }
        try
        {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser == null || loginUser.getUser() == null)
            {
                return false;
            }
            if (loginUser.getUser().isAdmin())
            {
                return true;
            }
            if (loginUser.getUser().getRoles() != null)
            {
                for (SysRole role : loginUser.getUser().getRoles())
                {
                    if (role != null && role.isAdmin())
                    {
                        return true;
                    }
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return false;
    }

    private boolean isCurrentOrderAllApproved(Long instanceId, int nodeOrder)
    {
        List<CrmWorkflowNode> nodes = workflowMapper.selectNodesByInstanceId(instanceId);
        return isOrderFullyApproved(nodes, nodeOrder);
    }

    /**
     * 指定回退目标：当前环节之前、该环节全部节点均已通过的主审节点（不含提交节点）。
     */
    private List<CrmWorkflowNode> resolveRollbackTargets(List<CrmWorkflowNode> nodes, Integer activeOrder)
    {
        List<CrmWorkflowNode> targets = new ArrayList<>();
        if (activeOrder == null || activeOrder <= 2)
        {
            return targets;
        }
        for (int order = 2; order < activeOrder; order++)
        {
            final int stageOrder = order;
            if (!isOrderFullyApproved(nodes, stageOrder))
            {
                continue;
            }
            CrmWorkflowNode primary = nodes.stream()
                .filter(n -> n.getNodeOrder() != null && n.getNodeOrder() == stageOrder
                    && "SINGLE".equals(n.getApprovalType()))
                .findFirst()
                .orElse(null);
            if (primary != null)
            {
                targets.add(primary);
            }
        }
        return targets;
    }

    private boolean isOrderFullyApproved(List<CrmWorkflowNode> nodes, int nodeOrder)
    {
        List<CrmWorkflowNode> sameOrder = nodes.stream()
            .filter(n -> n.getNodeOrder() != null && n.getNodeOrder() == nodeOrder)
            .collect(Collectors.toList());
        if (sameOrder.isEmpty())
        {
            return false;
        }
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
