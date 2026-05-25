-- ============================================================
-- 合同审批流程演示用户（Oracle 11g）
-- 用途：发起审批时选择「部门 / 财务 / 归档」审批人，分账号登录演示 BPMN 节点染色
-- 默认密码与若依初始账号相同：admin123
-- 执行前提：springBoot/sql/ry_20260417.sql、SQL/02_crm_menu.sql、SQL/04_crm_contract_workflow_menu.sql 已执行
-- ============================================================

-- 密码 BCrypt：admin123（与 admin/ry 相同）
-- $2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2

-- user_id  dept_id  user_name     nick_name    说明
-- 201      104      crm_sales     赵业务       销售/发起人（提交节点）
-- 202      104      crm_dept      张部门       部门审批人
-- 203      106      crm_finance   李财务       财务审批人
-- 204      106      crm_archive   王归档       归档审批人

insert into sys_user (
  user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar,
  password, status, del_flag, login_ip, login_date, pwd_update_date,
  create_by, create_time, update_by, update_time, remark
)
select 201, 104, 'crm_sales', '赵业务', '00', 'sales@demo.local', '13800002001', '0', '',
  '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate, sysdate,
  'admin', sysdate, '', null, 'CRM合同审批演示-发起人'
from dual
where not exists (select 1 from sys_user where user_id = 201 or user_name = 'crm_sales');

insert into sys_user (
  user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar,
  password, status, del_flag, login_ip, login_date, pwd_update_date,
  create_by, create_time, update_by, update_time, remark
)
select 202, 104, 'crm_dept', '张部门', '00', 'dept@demo.local', '13800002002', '0', '',
  '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate, sysdate,
  'admin', sysdate, '', null, 'CRM合同审批演示-部门审批'
from dual
where not exists (select 1 from sys_user where user_id = 202 or user_name = 'crm_dept');

insert into sys_user (
  user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar,
  password, status, del_flag, login_ip, login_date, pwd_update_date,
  create_by, create_time, update_by, update_time, remark
)
select 203, 106, 'crm_finance', '李财务', '00', 'finance@demo.local', '13800002003', '1', '',
  '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate, sysdate,
  'admin', sysdate, '', null, 'CRM合同审批演示-财务审批'
from dual
where not exists (select 1 from sys_user where user_id = 203 or user_name = 'crm_finance');

insert into sys_user (
  user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar,
  password, status, del_flag, login_ip, login_date, pwd_update_date,
  create_by, create_time, update_by, update_time, remark
)
select 204, 106, 'crm_archive', '王归档', '00', 'archive@demo.local', '13800002004', '0', '',
  '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate, sysdate,
  'admin', sysdate, '', null, 'CRM合同审批演示-归档审批'
from dual
where not exists (select 1 from sys_user where user_id = 204 or user_name = 'crm_archive');

-- 绑定「普通角色」(role_id=2)，已含 CRM 合同与工作流菜单权限
insert into sys_user_role (user_id, role_id)
select 201, 2 from dual where not exists (select 1 from sys_user_role where user_id = 201 and role_id = 2);
insert into sys_user_role (user_id, role_id)
select 202, 2 from dual where not exists (select 1 from sys_user_role where user_id = 202 and role_id = 2);
insert into sys_user_role (user_id, role_id)
select 203, 2 from dual where not exists (select 1 from sys_user_role where user_id = 203 and role_id = 2);
insert into sys_user_role (user_id, role_id)
select 204, 2 from dual where not exists (select 1 from sys_user_role where user_id = 204 and role_id = 2);

-- 若后续用 seq_sys_user.nextval 新增用户，请保证 MAX(user_id) >= 204，必要时在库中手工调高序列

commit;

-- ============================================================
-- 演示步骤（建议）
-- 1. admin 登录 → 合同管理 → 草稿合同 → 发起审批
--    部门审批人：张部门(crm_dept)
--    财务审批人：李财务(crm_finance)
--    归档审批人：王归档(crm_archive)
-- 2. crm_dept 登录 → 审批流程 → 通过（BPMN 部门节点变绿）
-- 3. crm_finance → 财务节点；crm_archive → 归档节点
-- 4. admin 仍可代审任意节点
-- ============================================================
