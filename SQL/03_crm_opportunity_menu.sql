-- ============================================================
-- CRM 商机管理菜单（阶段二增量脚本）
-- 执行前提：02_crm_menu.sql 已执行
-- ============================================================

insert into sys_menu values('2003', '商机管理', '2000', '3', 'opportunity', 'crm/opportunity/index', '', '', 1, 0, 'C', '0', '0', 'crm:opportunity:list', 'money', 'admin', sysdate, '', null, '商机管理菜单');
insert into sys_menu values('20031', '商机查询', '2003', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:opportunity:query',  '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20032', '商机新增', '2003', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:opportunity:add',    '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20033', '商机修改', '2003', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:opportunity:edit',   '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20034', '商机删除', '2003', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:opportunity:remove', '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20035', '商机导入', '2003', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:opportunity:import', '#', 'admin', sysdate, '', null, '');

insert into sys_role_menu values ('2', '2003');
insert into sys_role_menu values ('2', '20031');
insert into sys_role_menu values ('2', '20032');
insert into sys_role_menu values ('2', '20033');
insert into sys_role_menu values ('2', '20034');
insert into sys_role_menu values ('2', '20035');

commit;
