-- 统一 API 市场登记的接口地址为 / 开头（修复下架切面 URL 匹配失败）
-- 示例：crm/customer/list -> /crm/customer/list

update crm_api_info
   set api_url = case
         when api_url is null then api_url
         when substr(api_url, 1, 1) = '/' then api_url
         else '/' || api_url
       end,
       api_method = upper(trim(api_method)),
       update_time = sysdate
 where api_url is not null
   and (substr(api_url, 1, 1) != '/' or api_method != upper(trim(api_method)));

commit;
