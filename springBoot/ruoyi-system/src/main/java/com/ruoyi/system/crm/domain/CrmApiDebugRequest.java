package com.ruoyi.system.crm.domain;

/**
 * API 在线调试请求
 */
public class CrmApiDebugRequest
{
    /** 请求体 JSON 字符串（POST 时使用） */
    private String requestBody;

    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
}
