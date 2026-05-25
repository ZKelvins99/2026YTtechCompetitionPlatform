-- 商机 Excel 导入测试用客户数据（与 data/opportunity-import/*.xlsx 中客户名称一致）
-- 执行前请确认 seq_crm_customer 当前值，避免主键冲突

insert into crm_customer (id, customer_no, customer_name, level_id, industry, create_by, create_time)
select seq_crm_customer.nextval, 'IMP-TEST-A', 'CRM导入测试客户A', 2, '信息技术', 'admin', sysdate from dual
where not exists (select 1 from crm_customer where customer_name = 'CRM导入测试客户A');

insert into crm_customer (id, customer_no, customer_name, level_id, industry, create_by, create_time)
select seq_crm_customer.nextval, 'IMP-TEST-B', 'CRM导入测试客户B', 2, '智能制造', 'admin', sysdate from dual
where not exists (select 1 from crm_customer where customer_name = 'CRM导入测试客户B');

insert into crm_customer (id, customer_no, customer_name, level_id, industry, create_by, create_time)
select seq_crm_customer.nextval, 'IMP-TEST-C', 'CRM导入测试客户C', 3, '贸易', 'admin', sysdate from dual
where not exists (select 1 from crm_customer where customer_name = 'CRM导入测试客户C');

commit;
