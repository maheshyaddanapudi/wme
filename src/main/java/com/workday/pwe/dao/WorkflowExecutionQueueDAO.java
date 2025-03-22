package com.workday.pwe.dao;

import com.workday.pwe.enums.QueueStatus;
import com.workday.pwe.model.WorkflowExecutionQueue;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for workflow_execution_queue table operations.
 */
public class WorkflowExecutionQueueDAO {

    private static final Logger LOGGER = Logger.getLogger(WorkflowExecutionQueueDAO.class.getName());
    
    private final Connection connection;
    
    /**
     * Constructor with database connection
     * 
     * @param connection The database connection
     */
    public WorkflowExecutionQueueDAO(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.connection = connection;
    }
    
    /**
     * Add a workflow to the execution queue
     * 
     * @param queueEntry The queue entry to add
     * @return The ID of the created queue entry
     * @throws SQLException If a database error occurs
     */
    public UUID addToQueue(WorkflowExecutionQueue queueEntry) throws SQLException {
        final String sql = "INSERT INTO workflow_execution_queue " +
                           "(id, workflow_instance_id, status, priority, last_updated, created_at) " +
                           "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            UUID id = queueEntry.getId() != null ? queueEntry.getId() : UUID.randomUUID();
            
            stmt.setObject(1, id);
            stmt.setObject(2, queueEntry.getWorkflowInstanceId());
            stmt.setString(3, queueEntry.getStatus().name());
            stmt.setInt(4, queueEntry.getPriority());
            stmt.setTimestamp(5, Timestamp.valueOf(queueEntry.getLastUpdated() != null ? 
                                                  queueEntry.getLastUpdated() : LocalDateTime.now()));
            stmt.setTimestamp(6, Timestamp.valueOf(queueEntry.getCreatedAt() != null ? 
                                                  queueEntry.getCreatedAt() : LocalDateTime.now()));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Adding workflow to execution queue failed, no rows affected.");
            }
            
            return id;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding workflow to execution queue", e);
            throw e;
        }
    }
    
    /**
     * Update a queue entry's status
     * 
     * @param workflowInstanceId The workflow instance ID
     * @param status The new status
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int updateQueueStatus(UUID workflowInstanceId, QueueStatus status) throws SQLException {
        final String sql = "UPDATE workflow_execution_queue SET status = ?, last_updated = ? " +
                           "WHERE workflow_instance_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setObject(3, workflowInstanceId);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating queue status", e);
            throw e;
        }
    }
    
    /**
     * Update a queue entry's status and priority
     * 
     * @param workflowInstanceId The workflow instance ID
     * @param status The new status
     * @param priority The new priority
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int updateQueueStatusAndPriority(UUID workflowInstanceId, QueueStatus status, int priority) throws SQLException {
        final String sql = "UPDATE workflow_execution_queue SET status = ?, priority = ?, last_updated = ? " +
                           "WHERE workflow_instance_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, priority);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setObject(4, workflowInstanceId);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating queue status and priority", e);
            throw e;
        }
    }
    
    /**
     * Remove a workflow from the queue
     * 
     * @param workflowInstanceId The workflow instance ID
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int removeFromQueue(UUID workflowInstanceId) throws SQLException {
        final String sql = "DELETE FROM workflow_execution_queue WHERE workflow_instance_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowInstanceId);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error removing workflow from queue", e);
            throw e;
        }
    }
    
    /**
     * Check if a workflow is already in the queue
     * 
     * @param workflowInstanceId The workflow instance ID
     * @return true if the workflow is in the queue, false otherwise
     * @throws SQLException If a database error occurs
     */
    public boolean isWorkflowInQueue(UUID workflowInstanceId) throws SQLException {
        final String sql = "SELECT 1 FROM workflow_execution_queue WHERE workflow_instance_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowInstanceId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if workflow is in queue", e);
            throw e;
        }
    }
    
    /**
     * Get the latest poll time
     * 
     * @return The latest poll time, or null if no poll has been done
     * @throws SQLException If a database error occurs
     */
    public Timestamp getLatestPollTime() throws SQLException {
        final String sql = "SELECT MAX(last_updated) FROM workflow_execution_queue WHERE status = 'PROCESSING'";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getTimestamp(1);
            } else {
                return null;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting latest poll time", e);
            throw e;
        }
    }
    
    /**
     * Fetch queued workflows
     * 
     * @param includeProcessing Whether to include workflows with PROCESSING status
     * @return List of workflow instance IDs
     * @throws SQLException If a database error occurs
     */
    public List<String> fetchQueuedWorkflows(boolean includeProcessing) throws SQLException {
        String sql = "SELECT workflow_instance_id FROM workflow_execution_queue WHERE status = 'PENDING'";
        if (includeProcessing) {
            sql += " OR status = 'PROCESSING'";
        }
        sql += " ORDER BY priority DESC, created_at ASC";
        
        List<String> workflowIds = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                workflowIds.add(rs.getObject("workflow_instance_id").toString());
            }
            
            return workflowIds;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching queued workflows", e);
            throw e;
        }
    }
    
    /**
     * Get the queue entry for a workflow
     * 
     * @param workflowInstanceId The workflow instance ID
     * @return The queue entry, or null if not found
     * @throws SQLException If a database error occurs
     */
    public WorkflowExecutionQueue getQueueEntry(UUID workflowInstanceId) throws SQLException {
        final String sql = "SELECT id, workflow_instance_id, status, priority, last_updated, created_at " +
                           "FROM workflow_execution_queue WHERE workflow_instance_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowInstanceId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToQueueEntry(rs);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting queue entry", e);
            throw e;
        }
    }
    
    /**
     * Get all queue entries
     * 
     * @return List of all queue entries
     * @throws SQLException If a database error occurs
     */
    public List<WorkflowExecutionQueue> getAllQueueEntries() throws SQLException {
        final String sql = "SELECT id, workflow_instance_id, status, priority, last_updated, created_at " +
                           "FROM workflow_execution_queue ORDER BY priority DESC, created_at ASC";
        
        List<WorkflowExecutionQueue> entries = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                entries.add(mapResultSetToQueueEntry(rs));
            }
            
            return entries;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting all queue entries", e);
            throw e;
        }
    }
    
    /**
     * Map a result set row to a WorkflowExecutionQueue object
     * 
     * @param rs The result set
     * @return The mapped WorkflowExecutionQueue
     * @throws SQLException If a database error occurs
     */
    private WorkflowExecutionQueue mapResultSetToQueueEntry(ResultSet rs) throws SQLException {
        WorkflowExecutionQueue entry = new WorkflowExecutionQueue();
        
        entry.setId(UUID.fromString(rs.getString("id")));
        entry.setWorkflowInstanceId(UUID.fromString(rs.getString("workflow_instance_id")));
        entry.setStatus(QueueStatus.valueOf(rs.getString("status")));
        entry.setPriority(rs.getInt("priority"));
        entry.setLastUpdated(rs.getTimestamp("last_updated").toLocalDateTime());
        entry.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        return entry;
    }
    
    /**
     * Clean up old completed entries
     * 
     * @param olderThan Remove entries older than this time
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int cleanupOldEntries(LocalDateTime olderThan) throws SQLException {
        final String sql = "DELETE FROM workflow_execution_queue " +
                           "WHERE status IN ('COMPLETED', 'FAILED') AND last_updated < ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(olderThan));
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error cleaning up old queue entries", e);
            throw e;
        }
    }
}
