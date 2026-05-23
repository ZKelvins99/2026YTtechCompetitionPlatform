-- ============================================================
-- CRM 工作台菜单（阶段九增量脚本）
-- 表 crm_workbench_layout 已在 01_init_tables.sql 中创建
-- 执行前提：09_crm_api_menu.sql 已执行
-- ============================================================

insert into sys_menu values('2011', '工作台', '2000', '0', 'workbench', 'crm/workbench/index', '', '', 1, 0, 'C', '0', '0', 'crm:workbench:view', 'dashboard', 'admin', sysdate, '', null, 'CRM工作台');
insert into sys_menu values('20111', '布局查看', '2011', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:workbench:view', '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20112', '布局保存', '2011', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:workbench:save', '#', 'admin', sysdate, '', null, '');

insert into sys_role_menu values ('2', '2011');
insert into sys_role_menu values ('2', '20111');
insert into sys_role_menu values ('2', '20112');

commit;
