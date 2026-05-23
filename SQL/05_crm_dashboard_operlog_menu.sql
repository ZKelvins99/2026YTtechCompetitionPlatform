-- ============================================================
-- CRM 数据大屏与操作日志菜单（阶段三增量脚本）
-- 执行前提：04_crm_contract_workflow_menu.sql 已执行
-- ============================================================

insert into sys_menu values('2005', '数据大屏', '2000', '5', 'dashboard', 'crm/dashboard/index', '', '', 1, 0, 'C', '0', '0', 'crm:dashboard:view', 'chart', 'admin', sysdate, '', null, 'CRM数据监控大屏');
insert into sys_menu values('20051', '大屏查看', '2005', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:dashboard:view', '#', 'admin', sysdate, '', null, '');

insert into sys_menu values('2006', '操作日志', '2000', '6', 'operlog', 'crm/operlog/index', '', '', 1, 0, 'C', '0', '0', 'crm:operlog:list', 'log', 'admin', sysdate, '', null, 'CRM操作日志');
insert into sys_menu values('20061', '日志查询', '2006', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:operlog:query', '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20062', '日志列表', '2006', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:operlog:list',  '#', 'admin', sysdate, '', null, '');

insert into sys_role_menu values ('2', '2005');
insert into sys_role_menu values ('2', '20051');
insert into sys_role_menu values ('2', '2006');
insert into sys_role_menu values ('2', '20061');
insert into sys_role_menu values ('2', '20062');

commit;
