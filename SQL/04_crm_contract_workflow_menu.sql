-- ============================================================
-- CRM 合同管理与工作流菜单（阶段七增量脚本）
-- ============================================================

insert into sys_menu values('2004', '合同管理', '2000', '4', 'contract', 'crm/contract/index', '', '', 1, 0, 'C', '0', '0', 'crm:contract:list', 'documentation', 'admin', sysdate, '', null, '合同管理');
insert into sys_menu values('20041', '合同查询', '2004', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:contract:query',  '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20042', '合同新增', '2004', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:contract:add',    '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20043', '合同修改', '2004', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:contract:edit',   '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20044', '合同删除', '2004', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:contract:remove', '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20045', '发起审批', '2004', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:workflow:start',  '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20046', '流程查询', '2004', '6', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:workflow:query',  '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20047', '流程审批', '2004', '7', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:workflow:approve','#', 'admin', sysdate, '', null, '');

insert into sys_role_menu values ('2', '2004');
insert into sys_role_menu values ('2', '20041');
insert into sys_role_menu values ('2', '20042');
insert into sys_role_menu values ('2', '20043');
insert into sys_role_menu values ('2', '20044');
insert into sys_role_menu values ('2', '20045');
insert into sys_role_menu values ('2', '20046');
insert into sys_role_menu values ('2', '20047');

commit;
