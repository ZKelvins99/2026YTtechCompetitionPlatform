package com.ruoyi.system.crm.domain;

import java.util.Date;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * CRM 操作日志 crm_oper_log
 */
public class CrmOperLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String requestUrl;
    private String requestMethod;
    private String requestParams;
    private String responseResult;
    private String sqlStatements;
    private String traceId;
    private String operator;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date operateTime;

    private Long costMillis;
    /** 0成功 1失败 */
    private String status;
    private String errorMsg;

    /** 查询：开始时间 */
    private String beginTime;
    /** 查询：结束时间 */
    private String endTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequestUrl() { return requestUrl; }
    public void setRequestUrl(String requestUrl) { this.requestUrl = requestUrl; }
    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }
    public String getRequestParams() { return requestParams; }
    public void setRequestParams(String requestParams) { this.requestParams = requestParams; }
    public String getResponseResult() { return responseResult; }
    public void setResponseResult(String responseResult) { this.responseResult = responseResult; }
    public String getSqlStatements() { return sqlStatements; }
    public void setSqlStatements(String sqlStatements) { this.sqlStatements = sqlStatements; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public Date getOperateTime() { return operateTime; }
    public void setOperateTime(Date operateTime) { this.operateTime = operateTime; }
    public Long getCostMillis() { return costMillis; }
    public void setCostMillis(Long costMillis) { this.costMillis = costMillis; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public String getBeginTime() { return beginTime; }
    public void setBeginTime(String beginTime) { this.beginTime = beginTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    @Override
    public Map<String, Object> getParams()
    {
        return super.getParams();
    }
}
