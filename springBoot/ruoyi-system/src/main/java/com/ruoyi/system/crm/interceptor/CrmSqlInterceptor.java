package com.ruoyi.system.crm.interceptor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.crm.context.CrmTraceContext;

/**
 * 记录 MyBatis 执行的 SQL（含 SELECT）到链路追踪上下文
 */
@Component
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class }),
    @Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class }),
    @Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class })
})
public class CrmSqlInterceptor implements Interceptor
{
    private static final int MAX_SQL_LEN = 4000;

    @Override
    public Object intercept(Invocation invocation) throws Throwable
    {
        if (CrmTraceContext.getTraceId() != null)
        {
            MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
            if (!shouldRecord(ms))
            {
                return invocation.proceed();
            }
            Object parameter = invocation.getArgs()[1];
            BoundSql boundSql = resolveBoundSql(invocation, ms, parameter);
            String sql = formatSql(ms.getConfiguration(), boundSql);
            CrmTraceContext.addSql(sql);
        }
        return invocation.proceed();
    }

    private static BoundSql resolveBoundSql(Invocation invocation, MappedStatement ms, Object parameter)
    {
        if (invocation.getArgs().length == 6 && invocation.getArgs()[5] instanceof BoundSql)
        {
            return (BoundSql) invocation.getArgs()[5];
        }
        return ms.getBoundSql(parameter);
    }

    /** 跳过操作日志自身的写入，避免列表里只有 insert oper_log */
    private static boolean shouldRecord(MappedStatement ms)
    {
        String id = ms.getId();
        return id == null || !id.startsWith("com.ruoyi.system.crm.mapper.CrmOperLogMapper.");
    }

    private static String formatSql(Configuration configuration, BoundSql boundSql)
    {
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ").trim();
        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        if (mappings == null || mappings.isEmpty())
        {
            return truncate(sql);
        }
        Object parameterObject = boundSql.getParameterObject();
        TypeHandlerRegistry registry = configuration.getTypeHandlerRegistry();
        MetaObject meta = configuration.newMetaObject(parameterObject);
        for (ParameterMapping mapping : mappings)
        {
            String property = mapping.getProperty();
            Object value;
            if (boundSql.hasAdditionalParameter(property))
            {
                value = boundSql.getAdditionalParameter(property);
            }
            else if (parameterObject == null)
            {
                value = null;
            }
            else if (registry.hasTypeHandler(parameterObject.getClass()))
            {
                value = parameterObject;
            }
            else
            {
                value = meta.getValue(property);
            }
            sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(formatValue(value)));
        }
        return truncate(sql);
    }

    private static String formatValue(Object value)
    {
        if (value == null)
        {
            return "null";
        }
        if (value instanceof String)
        {
            return "'" + value.toString().replace("'", "''") + "'";
        }
        if (value instanceof Date)
        {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return "to_date('" + df.format((Date) value) + "', 'yyyy-mm-dd hh24:mi:ss')";
        }
        return value.toString();
    }

    private static String truncate(String sql)
    {
        if (StringUtils.isEmpty(sql))
        {
            return sql;
        }
        return sql.length() > MAX_SQL_LEN ? sql.substring(0, MAX_SQL_LEN) + "..." : sql;
    }

    @Override
    public Object plugin(Object target)
    {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties)
    {
    }
}
