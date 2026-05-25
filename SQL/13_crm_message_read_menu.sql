-- 消息标记已读权限
insert into sys_menu values('200733', '消息已读', '20073', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'crm:message:read', '#', 'admin', sysdate, '', null, '接收人标记消息已读');

insert into sys_role_menu values ('2', '200733');

commit;
