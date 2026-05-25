-- 修复审批中流程 current_node_id 指向已办节点导致无法审批的问题（Oracle 11g）
-- 将指针校正为当前最小待办环节中的第一条待办节点

update crm_workflow_instance i
set current_node_id = (
  select min(n.id)
  from crm_workflow_node n
  where n.instance_id = i.id
    and n.status = '0'
    and n.node_order > 1
    and n.node_order = (
      select min(n2.node_order)
      from crm_workflow_node n2
      where n2.instance_id = i.id
        and n2.status = '0'
        and n2.node_order > 1
    )
)
where i.status = '0'
  and exists (
    select 1 from crm_workflow_node p
    where p.instance_id = i.id and p.status = '0' and p.node_order > 1
  );

commit;
