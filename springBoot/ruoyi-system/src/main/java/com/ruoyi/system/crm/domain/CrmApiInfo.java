package com.ruoyi.system.crm.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CRM API 信息 crm_api_info
 */
public class CrmApiInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "API名称不能为空")
    @Size(max = 100, message = "API名称不能超过100个字符")
    private String apiName;

    @Size(max = 500, message = "API描述不能超过500个字符")
    private String apiDesc;

    @NotBlank(message = "接口地址不能为空")
    @Size(max = 200, message = "接口地址不能超过200个字符")
    private String apiUrl;

    @NotBlank(message = "请求方式不能为空")
    private String apiMethod;

    private String requestExample;
    private String responseExample;

    /** 0下架 1上架 */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApiName() { return apiName; }
    public void setApiName(String apiName) { this.apiName = apiName; }
    public String getApiDesc() { return apiDesc; }
    public void setApiDesc(String apiDesc) { this.apiDesc = apiDesc; }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getApiMethod() { return apiMethod; }
    public void setApiMethod(String apiMethod) { this.apiMethod = apiMethod; }
    public String getRequestExample() { return requestExample; }
    public void setRequestExample(String requestExample) { this.requestExample = requestExample; }
    public String getResponseExample() { return responseExample; }
    public void setResponseExample(String responseExample) { this.responseExample = responseExample; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
