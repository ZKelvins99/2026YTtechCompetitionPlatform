package com.ruoyi.system.crm.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.system.crm.config.CrmBehaviorImportProperties;
import com.ruoyi.system.crm.domain.CrmCustomerBehavior;

/**
 * 行为数据 JDBC 批量写入：PreparedStatement.addBatch / executeBatch，每批独立连接与事务。
 */
@Component
public class CrmBehaviorBulkInserter
{
    private static final String INSERT_SQL =
        "INSERT INTO crm_customer_behavior (id, customer_id, behavior_type, description, behavior_time, create_time) "
            + "VALUES (?, ?, ?, ?, ?, sysdate)";

    private static final String NEXT_IDS_SQL =
        "SELECT seq_crm_customer_behavior.NEXTVAL FROM dual CONNECT BY LEVEL <= ?";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CrmBehaviorImportProperties importProperties;

    public int batchSize()
    {
        return importProperties.getBatchSize();
    }

    /**
     * 单批完整导入：同一连接内取序列 ID + executeBatch + commit（每批独立事务）。
     */
    public void importBatchInTransaction(List<CrmCustomerBehavior> batch) throws SQLException
    {
        if (batch == null || batch.isEmpty())
        {
            return;
        }
        try (Connection connection = dataSource.getConnection())
        {
            prepareImportConnection(connection);
            List<Long> ids = fetchIdsOnConnection(connection, batch.size());
            try (PreparedStatement ps = connection.prepareStatement(INSERT_SQL))
            {
                for (int i = 0; i < batch.size(); i++)
                {
                    CrmCustomerBehavior item = batch.get(i);
                    item.setId(ids.get(i));
                    ps.setLong(1, item.getId());
                    ps.setLong(2, item.getCustomerId());
                    ps.setString(3, item.getBehaviorType());
                    ps.setString(4, item.getDescription());
                    ps.setTimestamp(5, new Timestamp(item.getBehaviorTime().getTime()));
                    ps.addBatch();
                }
                ps.executeBatch();
                connection.commit();
            }
            catch (SQLException e)
            {
                connection.rollback();
                throw e;
            }
        }
    }

    private void prepareImportConnection(Connection connection) throws SQLException
    {
        connection.setAutoCommit(false);
        applyOracleDefaultBatchValue(connection);
        disableNetworkTimeout(connection);
    }

    private void applyOracleDefaultBatchValue(Connection connection)
    {
        int batchValue = importProperties.getOracleDefaultBatchValue();
        try
        {
            Properties props = connection.getClientInfo();
            if (props == null)
            {
                props = new Properties();
            }
            props.setProperty("oracle.jdbc.defaultBatchValue", String.valueOf(batchValue));
            connection.setClientInfo(props);
        }
        catch (SQLException ignored)
        {
        }
    }

    private static void disableNetworkTimeout(Connection connection)
    {
        try
        {
            connection.setNetworkTimeout(Runnable::run, 0);
        }
        catch (SQLException ignored)
        {
        }
    }

    private List<Long> fetchIdsOnConnection(Connection connection, int count) throws SQLException
    {
        List<Long> ids = new ArrayList<>(count);
        try (PreparedStatement ps = connection.prepareStatement(NEXT_IDS_SQL))
        {
            ps.setInt(1, count);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    ids.add(rs.getLong(1));
                }
            }
        }
        if (ids.size() != count)
        {
            throw new SQLException("序列预取数量不足，期望 " + count + " 实际 " + ids.size());
        }
        return ids;
    }
}
