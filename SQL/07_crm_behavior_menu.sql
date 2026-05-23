-- ============================================================
-- CRM 海量行为数据菜单（阶段五增量脚本）
-- 表 crm_customer_behavior 已在 01_init_tables.sql 中创建
-- 执行前提：06_crm_message_menu.sql 已执行
-- ============================================================

insert into sys_menu values('2008', '客户行为记录', '2000', '8', 'behavior', 'crm/behavior/index', '', '', 1, 0, 'C', '0', '0', 'crm:behavior:list', 'table', 'admin', sysdate, '', null, '海量客户行为数据');
insert into sys_menu values('20081', '数据查询', '2008', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:behavior:list',     '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20082', '数据生成', '2008', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:behavior:generate', '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20083', '任务查询', '2008', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:behavior:query',    '#', 'admin', sysdate, '', null, '');

insert into sys_role_menu values ('2', '2008');
insert into sys_role_menu values ('2', '20081');
insert into sys_role_menu values ('2', '20082');
insert into sys_role_menu values ('2', '20083');

commit;
