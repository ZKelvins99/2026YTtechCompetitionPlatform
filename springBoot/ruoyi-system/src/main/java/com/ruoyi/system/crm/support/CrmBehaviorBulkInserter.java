package com.ruoyi.system.crm.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.system.crm.domain.CrmCustomerBehavior;
import com.ruoyi.system.crm.mapper.CrmCustomerBehaviorMapper;

/**
 * 行为数据批量写入：预取序列 ID + 每线程复用 JDBC batch（避免 INSERT ALL 大 SQL 解析开销）。
 */
@Component
public class CrmBehaviorBulkInserter
{
    /** JDBC batch 单批行数（Oracle 下与连接复用配合，兼顾吞吐与内存） */
    public static final int BATCH_SIZE = 2000;

    private static final int ID_PREFETCH_SIZE = 10000;

    private static final String INSERT_SQL =
        "INSERT INTO crm_customer_behavior (id, customer_id, behavior_type, description, behavior_time, create_time) "
            + "VALUES (?, ?, ?, ?, ?, sysdate)";

    private final Object idLock = new Object();

    private final List<Long> idBuffer = new ArrayList<>(ID_PREFETCH_SIZE);

    private int idBufferPos;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CrmCustomerBehaviorMapper crmCustomerBehaviorMapper;

    /**
     * 消费者线程专用：复用同一连接与 PreparedStatement，多批 executeBatch。
     */
    public InsertSession openSession() throws SQLException
    {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        PreparedStatement statement = connection.prepareStatement(INSERT_SQL);
        return new InsertSession(connection, statement);
    }

    public void insertBatch(InsertSession session, List<CrmCustomerBehavior> batch) throws SQLException
    {
        if (batch == null || batch.isEmpty())
        {
            return;
        }
        assignIds(batch);
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
    }

    public void clearIdPool()
    {
        synchronized (idLock)
        {
            idBuffer.clear();
            idBufferPos = 0;
        }
    }

    private void assignIds(List<CrmCustomerBehavior> batch)
    {
        int need = batch.size();
        synchronized (idLock)
        {
            ensureIds(need);
            for (int i = 0; i < need; i++)
            {
                batch.get(i).setId(idBuffer.get(idBufferPos++));
            }
        }
    }

    private void ensureIds(int need)
    {
        int available = idBuffer.size() - idBufferPos;
        while (available < need)
        {
            int fetch = Math.max(ID_PREFETCH_SIZE, need - available);
            List<Long> ids = crmCustomerBehaviorMapper.selectNextIds(fetch);
            idBuffer.addAll(ids);
            available = idBuffer.size() - idBufferPos;
        }
    }

    /** 将列表按 BATCH_SIZE 切分 */
    public static List<List<CrmCustomerBehavior>> partition(List<CrmCustomerBehavior> source)
    {
        List<List<CrmCustomerBehavior>> parts = new ArrayList<>();
        for (int i = 0; i < source.size(); i += BATCH_SIZE)
        {
            parts.add(new ArrayList<>(source.subList(i, Math.min(i + BATCH_SIZE, source.size()))));
        }
        return parts;
    }

    public static final class InsertSession implements AutoCloseable
    {
        private final Connection connection;
        private final PreparedStatement statement;

        private InsertSession(Connection connection, PreparedStatement statement)
        {
            this.connection = connection;
            this.statement = statement;
        }

        public void commit() throws SQLException
        {
            connection.commit();
        }

        public void rollback()
        {
            try
            {
                connection.rollback();
            }
            catch (SQLException ignored)
            {
            }
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
