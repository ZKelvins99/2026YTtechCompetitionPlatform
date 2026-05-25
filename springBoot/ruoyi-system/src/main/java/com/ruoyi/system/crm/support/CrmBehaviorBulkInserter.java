package com.ruoyi.system.crm.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.crm.domain.CrmCustomerBehavior;

/**
 * 行为数据 JDBC 批量写入：导入开始前一次性预取全部序列 ID，落库阶段不再访问数据库。
 */
@Component
public class CrmBehaviorBulkInserter
{
    private static final String INSERT_SQL =
        "INSERT INTO crm_customer_behavior (id, customer_id, behavior_type, description, behavior_time, create_time) "
            + "VALUES (?, ?, ?, ?, ?, sysdate)";

    private static final String NEXT_IDS_SQL =
        "SELECT seq_crm_customer_behavior.NEXTVAL FROM dual CONNECT BY LEVEL <= ?";

    /** 单次 CONNECT BY 上限，避免 Oracle 单次结果集过大 */
    private static final int ID_FETCH_CHUNK = 50000;

    private static final ThreadLocal<IdPool> ID_POOL = new ThreadLocal<>();

    @Autowired
    private DataSource dataSource;

    public static int batchSize()
    {
        return CrmBehaviorImportSettings.BATCH_SIZE;
    }

    /**
     * 导入/生成开始前调用：一次或分块拉取全部 ID，后续 assignIds 纯内存操作。
     */
    public void initIdPool(int totalRows) throws SQLException
    {
        if (totalRows <= 0)
        {
            throw new ServiceException("无有效数据可导入");
        }
        ID_POOL.set(new IdPool(fetchIdsViaJdbc(totalRows)));
    }

  /**
     * 从内存 ID 池分配，不再访问数据库。
     */
    public void assignIds(List<CrmCustomerBehavior> batch)
    {
        if (batch == null || batch.isEmpty())
        {
            return;
        }
        IdPool pool = ID_POOL.get();
        if (pool == null)
        {
            throw new IllegalStateException("ID 池未初始化，请先调用 initIdPool");
        }
        for (CrmCustomerBehavior item : batch)
        {
            item.setId(pool.next());
        }
    }

    public void clearIdPool()
    {
        ID_POOL.remove();
    }

    public InsertSession openSession() throws SQLException
    {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        applyImportConnectionHints(connection);
        PreparedStatement statement = connection.prepareStatement(INSERT_SQL);
        return new InsertSession(connection, statement);
    }

    public void insertBatch(InsertSession session, List<CrmCustomerBehavior> batch) throws SQLException
    {
        if (batch == null || batch.isEmpty())
        {
            return;
        }
        PreparedStatement ps = session.statement;
        for (CrmCustomerBehavior item : batch)
        {
            ps.setLong(1, item.getId());
            ps.setLong(2, item.getCustomerId());
            ps.setString(3, item.getBehaviorType());
            ps.setString(4, item.getDescription());
            ps.setTimestamp(5, new Timestamp(item.getBehaviorTime().getTime()));
            ps.addBatch();
        }
        ps.executeBatch();
        ps.clearBatch();
        session.markDirty();
    }

    private List<Long> fetchIdsViaJdbc(int totalRows) throws SQLException
    {
        List<Long> ids = new ArrayList<>(totalRows);
        try (Connection connection = dataSource.getConnection())
        {
            disableNetworkTimeout(connection);
            int remaining = totalRows;
            while (remaining > 0)
            {
                int chunk = Math.min(ID_FETCH_CHUNK, remaining);
                int before = ids.size();
                try (PreparedStatement ps = connection.prepareStatement(NEXT_IDS_SQL))
                {
                    ps.setInt(1, chunk);
                    try (ResultSet rs = ps.executeQuery())
                    {
                        while (rs.next())
                        {
                            ids.add(rs.getLong(1));
                        }
                    }
                }
                if (ids.size() - before != chunk)
                {
                    throw new SQLException("序列预取数量不足，期望 " + chunk + " 实际 " + (ids.size() - before));
                }
                remaining -= chunk;
            }
        }
        return ids;
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

    private static void applyImportConnectionHints(Connection connection) throws SQLException
    {
        connection.setAutoCommit(false);
        disableNetworkTimeout(connection);
    }

    public static List<List<CrmCustomerBehavior>> partition(List<CrmCustomerBehavior> source)
    {
        int size = CrmBehaviorImportSettings.BATCH_SIZE;
        List<List<CrmCustomerBehavior>> parts = new ArrayList<>();
        for (int i = 0; i < source.size(); i += size)
        {
            parts.add(new ArrayList<>(source.subList(i, Math.min(i + size, source.size()))));
        }
        return parts;
    }

    private static final class IdPool
    {
        private final List<Long> ids;
        private int cursor;

        private IdPool(List<Long> ids)
        {
            this.ids = ids;
        }

        long next()
        {
            if (cursor >= ids.size())
            {
                throw new IllegalStateException("ID 池已耗尽");
            }
            return ids.get(cursor++);
        }
    }

    public static final class InsertSession implements AutoCloseable
    {
        private final Connection connection;
        private final PreparedStatement statement;
        private boolean dirty;

        private InsertSession(Connection connection, PreparedStatement statement)
        {
            this.connection = connection;
            this.statement = statement;
        }

        void markDirty()
        {
            dirty = true;
        }

        public void commitIfDirty() throws SQLException
        {
            if (dirty)
            {
                connection.commit();
                dirty = false;
            }
        }

        public void rollback()
        {
            try
            {
                if (dirty)
                {
                    connection.rollback();
                }
            }
            catch (SQLException ignored)
            {
            }
            dirty = false;
        }

        @Override
        public void close()
        {
            try
            {
                statement.close();
            }
            catch (SQLException ignored)
            {
            }
            try
            {
                connection.close();
            }
            catch (SQLException ignored)
            {
            }
        }
    }
}
