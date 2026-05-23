-- ============================================================
-- CRM 系统数据库初始化脚本（Oracle 11g）
-- 包含：码表、业务表、序列及码表初始数据
-- ============================================================

-- ==================== 一、码表 ====================

-- 1.1 客户等级码表
create sequence seq_crm_customer_level increment by 1 start with 10 nomaxvalue nominvalue cache 20;

create table crm_customer_level (
  id             number(10)      not null,
  level_code     varchar2(20)    not null,
  level_name     varchar2(50)    not null,
  discount_rate  number(3,2)     default 1.00,
  sort_order     number(5)       default 0,
  status         char(1)         default '0',
  create_by      varchar2(64),
  create_time    date
);
alter table crm_customer_level add constraint pk_crm_customer_level primary key (id);
alter table crm_customer_level add constraint uk_crm_level_code unique (level_code);

insert into crm_customer_level values (1, 'VIP',    'VIP客户',   0.85, 1, '0', 'admin', sysdate);
insert into crm_customer_level values (2, 'NORMAL', '普通客户',  1.00, 2, '0', 'admin', sysdate);
insert into crm_customer_level values (3, 'POTENTIAL', '潜在客户', 1.00, 3, '0', 'admin', sysdate);

-- 1.2 商机阶段码表
create sequence seq_crm_opportunity_stage increment by 1 start with 10 nomaxvalue nominvalue cache 20;

create table crm_opportunity_stage (
  id           number(10)    not null,
  stage_code   varchar2(20)  not null,
  stage_name   varchar2(50)  not null,
  probability  number(3)     default 0,
  sort_order   number(5)     default 0,
  status       char(1)       default '0',
  create_time  date
);
alter table crm_opportunity_stage add constraint pk_crm_opportunity_stage primary key (id);
alter table crm_opportunity_stage add constraint uk_crm_stage_code unique (stage_code);

insert into crm_opportunity_stage values (1, 'CONTACT',   '初步接触', 10,  1, '0', sysdate);
insert into crm_opportunity_stage values (2, 'ANALYSIS',  '需求分析', 30,  2, '0', sysdate);
insert into crm_opportunity_stage values (3, 'QUOTE',     '报价',     50,  3, '0', sysdate);
insert into crm_opportunity_stage values (4, 'NEGOTIATE', '谈判',     70,  4, '0', sysdate);
insert into crm_opportunity_stage values (5, 'WIN',       '成交',     100, 5, '0', sysdate);
insert into crm_opportunity_stage values (6, 'LOST',      '丢单',     0,   6, '0', sysdate);

-- 1.3 联系方式类型码表
create sequence seq_crm_contact_type increment by 1 start with 10 nomaxvalue nominvalue cache 20;

create table crm_contact_type (
  id           number(10)    not null,
  type_code    varchar2(20)  not null,
  type_name    varchar2(50)  not null,
  icon         varchar2(100),
  sort_order   number(5)     default 0,
  status       char(1)       default '0',
  create_time  date
);
alter table crm_contact_type add constraint pk_crm_contact_type primary key (id);
alter table crm_contact_type add constraint uk_crm_type_code unique (type_code);

insert into crm_contact_type values (1, 'MOBILE',  '手机', 'phone',   1, '0', sysdate);
insert into crm_contact_type values (2, 'TEL',     '座机', 'tel',     2, '0', sysdate);
insert into crm_contact_type values (3, 'EMAIL',   '邮箱', 'email',   3, '0', sysdate);
insert into crm_contact_type values (4, 'WECHAT',  '微信', 'wechat',  4, '0', sysdate);
insert into crm_contact_type values (5, 'QQ',      'QQ',   'qq',      5, '0', sysdate);

-- 1.4 合同状态码表
create sequence seq_crm_contract_status increment by 1 start with 10 nomaxvalue nominvalue cache 20;

create table crm_contract_status (
  id            number(10)    not null,
  status_code   varchar2(20)  not null,
  status_name   varchar2(50)  not null,
  sort_order    number(5)     default 0,
  status        char(1)       default '0',
  create_time   date
);
alter table crm_contract_status add constraint pk_crm_contract_status primary key (id);
alter table crm_contract_status add constraint uk_crm_status_code unique (status_code);

insert into crm_contract_status values (1, 'DRAFT',     '草稿',   1, '0', sysdate);
insert into crm_contract_status values (2, 'APPROVING',  '审批中', 2, '0', sysdate);
insert into crm_contract_status values (3, 'ACTIVE',    '已生效', 3, '0', sysdate);
insert into crm_contract_status values (4, 'TERMINATED','已终止', 4, '0', sysdate);
insert into crm_contract_status values (5, 'ARCHIVED',  '已归档', 5, '0', sysdate);

-- ==================== 二、核心业务表 ====================

-- 2.1 客户表
create sequence seq_crm_customer increment by 1 start with 100 nomaxvalue nominvalue cache 20;

create table crm_customer (
  id             number(10)      not null,
  customer_no    varchar2(32)    not null,
  customer_name  varchar2(100)   not null,
  level_id       number(10),
  industry       varchar2(50),
  province       varchar2(20),
  city           varchar2(20),
  address        varchar2(200),
  phone          varchar2(20),
  email          varchar2(100),
  remark         varchar2(500),
  create_by      varchar2(64),
  create_time    date,
  update_by      varchar2(64),
  update_time    date
);
alter table crm_customer add constraint pk_crm_customer primary key (id);
alter table crm_customer add constraint uk_crm_customer_no unique (customer_no);

-- 2.2 联系人表
create sequence seq_crm_contact increment by 1 start with 100 nomaxvalue nominvalue cache 20;

create table crm_contact (
  id                number(10)    not null,
  customer_id       number(10)    not null,
  contact_name      varchar2(50)  not null,
  contact_type_id   number(10),
  contact_value     varchar2(100),
  position          varchar2(50),
  is_primary        char(1)       default '0',
  create_time       date
);
alter table crm_contact add constraint pk_crm_contact primary key (id);

-- 2.3 商机表
create sequence seq_crm_opportunity increment by 1 start with 100 nomaxvalue nominvalue cache 20;

create table crm_opportunity (
  id                   number(10)      not null,
  opportunity_name     varchar2(100)   not null,
  customer_id          number(10)      not null,
  stage_id             number(10),
  estimated_amount     number(15,2),
  expected_close_date  date,
  create_by            varchar2(64),
  create_time          date,
  update_time          date
);
alter table crm_opportunity add constraint pk_crm_opportunity primary key (id);

-- 2.4 合同表
create sequence seq_crm_contract increment by 1 start with 100 nomaxvalue nominvalue cache 20;

create table crm_contract (
  id               number(10)      not null,
  contract_no      varchar2(32)    not null,
  contract_name    varchar2(100)   not null,
  customer_id      number(10),
  opportunity_id   number(10),
  contract_amount  number(15,2)    not null,
  status_id        number(10),
  sign_date        date,
  start_date       date,
  end_date         date,
  content          clob,
  create_by        varchar2(64),
  create_time      date,
  update_time      date
);
alter table crm_contract add constraint pk_crm_contract primary key (id);
alter table crm_contract add constraint uk_crm_contract_no unique (contract_no);

-- 2.5 消息模板表
create sequence seq_crm_message_template increment by 1 start with 100 nomaxvalue nominvalue cache 20;

create table crm_message_template (
  id              number(10)      not null,
  template_name   varchar2(100)   not null,
  template_type   varchar2(20)    not null,
  title           varchar2(200)   not null,
  content         clob            not null,
  variables       varchar2(500),
  status          char(1)         default '0',
  create_time     date
);
alter table crm_message_template add constraint pk_crm_message_template primary key (id);

-- 2.6 消息记录表
create sequence seq_crm_message_record increment by 1 start with 100 nomaxvalue nominvalue cache 20;

create table crm_message_record (
  id            number(10)      not null,
  template_id   number(10),
  sender_id     number(10),
  receiver_id   number(10),
  title         varchar2(200)   not null,
  content       clob,
  status        char(1)         default '0',
  send_time     date,
  recall_time   date
);
alter table crm_message_record add constraint pk_crm_message_record primary key (id);

-- 2.7 API信息表
create sequence seq_crm_api_info increment by 1 start with 100 nomaxvalue nominvalue cache 20;

create table crm_api_info (
  id                 number(10)      not null,
  api_name           varchar2(100)   not null,
  api_desc           varchar2(500),
  api_url            varchar2(200)   not null,
  api_method         varchar2(10)    not null,
  request_example    clob,
  response_example   clob,
  status             char(1)         default '1',
  create_by          varchar2(64),
  create_time        date,
  update_time        date
);
alter table crm_api_info add constraint pk_crm_api_info primary key (id);

-- 2.8 工作流实例表
create sequence seq_crm_workflow_instance increment by 1 start with 100 nomaxvalue nominvalue cache 20;

create table crm_workflow_instance (
  id                number(10)    not null,
  process_type      varchar2(20)  not null,
  business_id       number(10)    not null,
  business_no       varchar2(32),
  current_node_id   number(10),
  status            char(1)       default '0',
  start_user_id     number(10),
  start_time        date,
  end_time          date
);
alter table crm_workflow_instance add constraint pk_crm_workflow_instance primary key (id);

-- 2.9 工作流节点表
create sequence seq_crm_workflow_node increment by 1 start with 100 nomaxvalue nominvalue cache 20;

create table crm_workflow_node (
  id              number(10)    not null,
  instance_id     number(10)    not null,
  node_name       varchar2(50)  not null,
  node_order      number(3)     not null,
  approver_id     number(10),
  approval_type   varchar2(20)  default 'SINGLE',
  status          char(1)       default '0',
  opinion         varchar2(500),
  approve_time    date
);
alter table crm_workflow_node add constraint pk_crm_workflow_node primary key (id);

-- 2.10 操作日志扩展表
create sequence seq_crm_oper_log increment by 1 start with 100 nomaxvalue nominvalue cache 20;

create table crm_oper_log (
  id                number(10)      not null,
  request_url       varchar2(200),
  request_method    varchar2(10),
  request_params    clob,
  response_result   clob,
  sql_statements    clob,
  trace_id          varchar2(64),
  operator          varchar2(64),
  operate_time      date,
  cost_millis       number(10),
  status            char(1)         default '0',
  error_msg         varchar2(2000)
);
alter table crm_oper_log add constraint pk_crm_oper_log primary key (id);

-- 2.11 客户行为记录表（海量数据）
create sequence seq_crm_customer_behavior increment by 1 start with 1 nomaxvalue nominvalue cache 100;

create table crm_customer_behavior (
  id              number(20)      not null,
  customer_id     number(10),
  behavior_type   varchar2(50),
  description     varchar2(500),
  behavior_time   date,
  create_time     date          default sysdate
);
alter table crm_customer_behavior add constraint pk_crm_customer_behavior primary key (id);

-- 2.12 用户画像统计表
create sequence seq_crm_user_profile increment by 1 start with 100 nomaxvalue nominvalue cache 20;

create table crm_user_profile (
  id                      number(10)      not null,
  user_id                 number(10),
  customer_count          number(10)      default 0,
  opportunity_count       number(10)      default 0,
  win_rate                number(5,2)     default 0,
  total_amount            number(18,2)    default 0,
  industry_distribution   clob,
  region_distribution     clob,
  monthly_performance     clob,
  update_time             date
);
alter table crm_user_profile add constraint pk_crm_user_profile primary key (id);

-- 2.13 工作台布局表
create sequence seq_crm_workbench_layout increment by 1 start with 100 nomaxvalue nominvalue cache 20;

create table crm_workbench_layout (
  id            number(10)    not null,
  user_id       number(10)    not null,
  layout_json   clob          not null,
  update_time   date
);
alter table crm_workbench_layout add constraint pk_crm_workbench_layout primary key (id);
alter table crm_workbench_layout add constraint uk_crm_workbench_user unique (user_id);

commit;
