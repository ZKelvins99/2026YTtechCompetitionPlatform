-- ============================================================
-- CRM 用户画像菜单（阶段六增量脚本）
-- 表 crm_user_profile 已在 01_init_tables.sql 中创建
-- 执行前提：07_crm_behavior_menu.sql 已执行
-- ============================================================

insert into sys_menu values('20053', '用户画像', '2000', '10', 'dashboard/profile', 'crm/dashboard/profile', '', '', 1, 0, 'C', '0', '0', 'crm:profile:view', 'peoples', 'admin', sysdate, '', null, 'CRM用户画像');
insert into sys_menu values('200531', '画像查看', '20053', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:profile:view', '#', 'admin', sysdate, '', null, '');

insert into sys_role_menu values ('2', '20053');
insert into sys_role_menu values ('2', '200531');

commit;
