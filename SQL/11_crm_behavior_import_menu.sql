-- ============================================================
-- CRM 客户行为 Excel 导入/清空权限（增量脚本）
-- 执行前提：07_crm_behavior_menu.sql 已执行
-- ============================================================

insert into sys_menu values('20084', 'Excel导入', '2008', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:behavior:import', '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20085', '清空数据', '2008', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:behavior:remove', '#', 'admin', sysdate, '', null, '');

insert into sys_role_menu values ('2', '20084');
insert into sys_role_menu values ('2', '20085');

commit;
