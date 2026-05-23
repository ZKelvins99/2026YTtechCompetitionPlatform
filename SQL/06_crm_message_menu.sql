-- ============================================================
-- CRM 消息中心菜单（阶段四增量脚本）
-- 执行前提：05_crm_dashboard_operlog_menu.sql 已执行
-- ============================================================

insert into sys_menu values('2007', '消息中心', '2000', '7', 'message', null, '', '', 1, 0, 'M', '0', '0', '', 'message', 'admin', sysdate, '', null, 'CRM消息中心');
insert into sys_menu values('20071', '模板管理', '2007', '1', 'template', 'crm/message/template', '', '', 1, 0, 'C', '0', '0', 'crm:message:template:list', 'edit', 'admin', sysdate, '', null, '消息模板管理');
insert into sys_menu values('20072', '发送消息', '2007', '2', 'send',     'crm/message/send',     '', '', 1, 0, 'C', '0', '0', 'crm:message:send',        'email','admin', sysdate, '', null, '发送CRM消息');
insert into sys_menu values('20073', '消息记录', '2007', '3', 'record',   'crm/message/record',   '', '', 1, 0, 'C', '0', '0', 'crm:message:record:list', 'list', 'admin', sysdate, '', null, '消息发送记录');

insert into sys_menu values('200711', '模板查询', '20071', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:message:template:query',  '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('200712', '模板新增', '20071', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:message:template:add',    '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('200713', '模板修改', '20071', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:message:template:edit',   '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('200714', '模板删除', '20071', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:message:template:remove', '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('200731', '消息撤回', '20073', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:message:recall', '#', 'admin', sysdate, '', null, '');
insert into sys_menu values('200732', '消息重发', '20073', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:message:resend', '#', 'admin', sysdate, '', null, '');

insert into sys_role_menu values ('2', '2007');
insert into sys_role_menu values ('2', '20071');
insert into sys_role_menu values ('2', '20072');
insert into sys_role_menu values ('2', '20073');
insert into sys_role_menu values ('2', '200711');
insert into sys_role_menu values ('2', '200712');
insert into sys_role_menu values ('2', '200713');
insert into sys_role_menu values ('2', '200714');
insert into sys_role_menu values ('2', '200731');
insert into sys_role_menu values ('2', '200732');

commit;
