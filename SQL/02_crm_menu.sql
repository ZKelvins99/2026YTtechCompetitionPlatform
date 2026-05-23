-- ============================================================
-- CRM 系统菜单与权限初始化（Oracle 11g）
-- 执行前提：若依基础库已初始化
-- ============================================================

-- 一级目录：CRM系统
insert into sys_menu values('2000', 'CRM系统', '0', '5', 'crm', null, '', '', 1, 0, 'M', '0', '0', '', 'peoples', 'admin', sysdate, '', null, 'CRM客户关系管理');

-- 客户管理
insert into sys_menu values('2001', '客户管理', '2000', '1', 'customer', 'crm/customer/index', '', '', 1, 0, 'C', '0', '0', 'crm:customer:list', 'user', 'admin', sysdate, '', null, '客户管理菜单');
insert into sys_menu values('20011', '客户查询', '2001', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:customer:query',  '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20012', '客户新增', '2001', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:customer:add',    '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20013', '客户修改', '2001', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:customer:edit',   '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20014', '客户删除', '2001', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:customer:remove', '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20015', '客户导出', '2001', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:customer:export', '#', 'admin', sysdate, '', null, '');

-- 联系人管理
insert into sys_menu values('2002', '联系人管理', '2000', '2', 'contact', 'crm/contact/index', '', '', 1, 0, 'C', '0', '0', 'crm:contact:list', 'phone', 'admin', sysdate, '', null, '联系人管理菜单');
insert into sys_menu values('20021', '联系人查询', '2002', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:contact:query',  '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20022', '联系人新增', '2002', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:contact:add',    '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20023', '联系人修改', '2002', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:contact:edit',   '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20024', '联系人删除', '2002', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:contact:remove', '#', 'admin', sysdate, '', null, '');

-- 为普通角色(role_id=2)授权 CRM 菜单
insert into sys_role_menu values ('2', '2000');
insert into sys_role_menu values ('2', '2001');
insert into sys_role_menu values ('2', '20011');
insert into sys_role_menu values ('2', '20012');
insert into sys_role_menu values ('2', '20013');
insert into sys_role_menu values ('2', '20014');
insert into sys_role_menu values ('2', '20015');
insert into sys_role_menu values ('2', '2002');
insert into sys_role_menu values ('2', '20021');
insert into sys_role_menu values ('2', '20022');
insert into sys_role_menu values ('2', '20023');
insert into sys_role_menu values ('2', '20024');

commit;
