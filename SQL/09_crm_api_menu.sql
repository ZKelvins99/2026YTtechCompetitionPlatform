-- ============================================================
-- CRM API 服务市场菜单（阶段八增量脚本）
-- 表 crm_api_info 已在 01_init_tables.sql 中创建
-- 执行前提：08_crm_profile_menu.sql 已执行
-- ============================================================

insert into sys_menu values('2010', 'API服务市场', '2000', '11', 'api', 'crm/api/index', '', '', 1, 0, 'C', '0', '0', 'crm:api:list', 'link', 'admin', sysdate, '', null, 'CRM API服务市场');
insert into sys_menu values('20101', 'API查询', '2010', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:api:query',  '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20102', 'API发布', '2010', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:api:add',    '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20103', 'API编辑', '2010', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:api:edit',   '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20104', 'API删除', '2010', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:api:remove', '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20105', '上下架',   '2010', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:api:online', '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('20106', '在线调试', '2010', '6', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:api:debug',  '#', 'admin', sysdate, '', null, '');

insert into sys_role_menu values ('2', '2010');
insert into sys_role_menu values ('2', '20101');
insert into sys_role_menu values ('2', '20102');
insert into sys_role_menu values ('2', '20103');
insert into sys_role_menu values ('2', '20104');
insert into sys_role_menu values ('2', '20105');
insert into sys_role_menu values ('2', '20106');

-- 示例 API：客户总数查询
insert into crm_api_info (id, api_name, api_desc, api_url, api_method, request_example, response_example, status, create_by, create_time)
values (seq_crm_api_info.nextval, '客户总数查询', '返回当前 CRM 客户总数', '/crm/api/demo/customer-count', 'GET',
        '', '{"code":200,"data":{"count":100}}', '1', 'admin', sysdate);

commit;
