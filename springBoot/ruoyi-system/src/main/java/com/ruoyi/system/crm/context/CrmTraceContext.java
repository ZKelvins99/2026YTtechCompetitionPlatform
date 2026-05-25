package com.ruoyi.system.crm.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.alibaba.fastjson2.JSON;

/**
 * CRM 请求链路追踪上下文（ThreadLocal）
 */
public class CrmTraceContext
{
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> SQL_LIST = ThreadLocal.withInitial(ArrayList::new);

    public static void setTraceId(String traceId)
    {
        TRACE_ID.set(traceId);
        SQL_LIST.remove();
    }

    public static String getTraceId()
    {
        return TRACE_ID.get();
    }

    public static void addSql(String sql)
    {
        if (sql != null && !sql.isBlank())
        {
            SQL_LIST.get().add(sql);
        }
    }

    public static List<String> getSqlList()
    {
        return Collections.unmodifiableList(new ArrayList<>(SQL_LIST.get()));
    }

    public static String getSqlStatementsJson()
    {
        List<String> list = SQL_LIST.get();
        if (list == null || list.isEmpty())
        {
            return "[]";
        }
        return JSON.toJSONString(list);
    }

    public static void clear()
    {
        TRACE_ID.remove();
        SQL_LIST.remove();
    }
}
