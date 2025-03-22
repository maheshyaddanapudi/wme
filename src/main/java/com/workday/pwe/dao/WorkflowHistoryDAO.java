package com.workday.pwe.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workday.pwe.model.WorkflowHistory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for workflow_history table operations.
 */
public class WorkflowHistoryDAO {

    private static final Logger LOGGER = Logger.getLogger(WorkflowHistoryDAO.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final Connection connection;
    
    /**
     * Constructor with database connection
     * 
     * @param connection The database connection
     */
    public WorkflowHistoryDAO(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.connection = connection;
    }
    
    /**
     * Add a history record
     * 
     * @param historyRecord The history record to add
     * @return The ID of the created history record
     * @throws SQLException If a database error occurs
     */
    public UUID addHistoryRecord(WorkflowHistory historyRecord) throws SQLException {
        final String sql = "INSERT INTO workflow_history " +
                           "(id, workflow_instance_id, entity_type, entity_id, change_type, details_json, timestamp, username) " +
                           "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            UUID id = historyRecord.getId() != null ? historyRecord.getId() : UUID.randomUUID();
            
            stmt.setObject(1, id);
            stmt.setObject(2, historyRecord.getWorkflowInstanceId());
            stmt.setString(3, historyRecord.getEntityType());
            stmt.setObject(4, historyRecord.getEntityId());
            stmt.setString(5, historyRecord.getChangeType());
            stmt.setString(6, historyRecord.getDetailsJson() != null ? historyRecord.getDetailsJson().toString() : null);
            stmt.setTimestamp(7, Timestamp.valueOf(historyRecord.getTimestamp() != null ? 
                                                  historyRecord.getTimestamp() : LocalDateTime.now()));
            stmt.setString(8, historyRecord.getUsername());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Adding history record failed, no rows affected.");
            }
            
            return id;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding history record", e);
            throw e;
        }
    }
    
    /**
     * Get history records for a workflow
     * 
     * @param workflowInstanceId The workflow instance ID
     * @return List of history records
     * @throws SQLException If a database error occurs
     */
    public List<WorkflowHistory> getHistoryForWorkflow(UUID workflowInstanceId) throws SQLException {
        final String sql = "SELECT id, workflow_instance_id, entity_type, entity_id, change_type, details_json, timestamp, username " +
                           "FROM workflow_history WHERE workflow_instance_id = ? " +
                           "ORDER BY timestamp DESC";
        
        List<WorkflowHistory> historyRecords = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowInstanceId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    historyRecords.add(mapResultSetToHistoryRecord(rs));
                }
            }
            
            return historyRecords;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting history for workflow", e);
            throw e;
        }
    }
    
    /**
     * Get history records for an entity
     * 
     * @param entityType The entity type
     * @param entityId The entity ID
     * @return List of history records
     * @throws SQLException If a database error occurs
     */
    public List<WorkflowHistory> getHistoryForEntity(String entityType, UUID entityId) throws SQLException {
        final String sql = "SELECT id, workflow_instance_id, entity_type, entity_id, change_type, details_json, timestamp, username " +
                           "FROM workflow_history WHERE entity_type = ? AND entity_id = ? " +
                           "ORDER BY timestamp DESC";
        
        List<WorkflowHistory> historyRecords = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, entityType);
            stmt.setObject(2, entityId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    historyRecords.add(mapResultSetToHistoryRecord(rs));
                }
            }
            
            return historyRecords;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting history for entity", e);
            throw e;
        }
    }
    
    /**
     * Get history records by change type
     * 
     * @param workflowInstanceId The workflow instance ID
     * @param changeType The change type
     * @return List of history records
     * @throws SQLException If a database error occurs
     */
    public List<WorkflowHistory> getHistoryByChangeType(UUID workflowInstanceId, String changeType) throws SQLException {
        final String sql = "SELECT id, workflow_instance_id, entity_type, entity_id, change_type, details_json, timestamp, username " +
                           "FROM workflow_history WHERE workflow_instance_id = ? AND change_type = ? " +
                           "ORDER BY timestamp DESC";
        
        List<WorkflowHistory> historyRecords = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowInstanceId);
            stmt.setString(2, changeType);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    historyRecords.add(mapResultSetToHistoryRecord(rs));
                }
            }
            
            return historyRecords;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting history by change type", e);
            throw e;
        }
    }
    
    /**
     * Get recent history records
     * 
     * @param limit Maximum number of records to return
     * @return List of recent history records
     * @throws SQLException If a database error occurs
     */
    public List<WorkflowHistory> getRecentHistory(int limit) throws SQLException {
        final String sql = "SELECT id, workflow_instance_id, entity_type, entity_id, change_type, details_json, timestamp, username " +
                           "FROM workflow_history ORDER BY timestamp DESC LIMIT ?";
        
        List<WorkflowHistory> historyRecords = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    historyRecords.add(mapResultSetToHistoryRecord(rs));
                }
            }
            
            return historyRecords;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting recent history", e);
            throw e;
        }
    }
    
    /**
     * Delete history records for a workflow
     * 
     * @param workflowInstanceId The workflow instance ID
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int deleteHistoryForWorkflow(UUID workflowInstanceId) throws SQLException {
        final String sql = "DELETE FROM workflow_history WHERE workflow_instance_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowInstanceId);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting history for workflow", e);
            throw e;
        }
    }
    
    /**
     * Archive old history records
     * 
     * @param olderThan Records older than this time
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int archiveOldHistory(LocalDateTime olderThan) throws SQLException {
        // In a real implementation, this would move records to an archive table
        // For now, we'll just simulate it by returning a count
        final String sql = "SELECT COUNT(*) FROM workflow_history WHERE timestamp < ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(olderThan));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    return 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error archiving old history", e);
            throw e;
        }
    }
    
    /**
     * Map a result set row to a WorkflowHistory object
     * 
     * @param rs The result set
     * @return The mapped WorkflowHistory
     * @throws SQLException If a database error occurs
     */
    private WorkflowHistory mapResultSetToHistoryRecord(ResultSet rs) throws SQLException {
        WorkflowHistory historyRecord = new WorkflowHistory();
        
        historyRecord.setId(UUID.fromString(rs.getString("id")));
        historyRecord.setWorkflowInstanceId(UUID.fromString(rs.getString("workflow_instance_id")));
        historyRecord.setEntityType(rs.getString("entity_type"));
        historyRecord.setEntityId(UUID.fromString(rs.getString("entity_id")));
        historyRecord.setChangeType(rs.getString("change_type"));
        historyRecord.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        historyRecord.setUsername(rs.getString("username"));
        
        // Parse the JSON details
        try {
            String jsonStr = rs.getString("details_json");
            if (jsonStr != null) {
                JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonStr);
                historyRecord.setDetailsJson(jsonNode);
            }
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Error parsing details JSON", e);
            throw new SQLException("Error parsing details JSON", e);
        }
        
        return historyRecord;
    }
    
    /**
     * Add a status change history record
     * 
     * @param workflowInstanceId The workflow instance ID
     * @param entityType The entity type
     * @param entityId The entity ID
     * @param oldStatus The old status
     * @param newStatus The new status
     * @param username The user who made the change
     * @return The ID of the created history record
     * @throws SQLException If a database error occurs
     */
    public UUID addStatusChangeRecord(UUID workflowInstanceId, String entityType, UUID entityId, 
                                    String oldStatus, String newStatus, String username) throws SQLException {
        try {
            // Create details JSON
            com.fasterxml.jackson.databind.node.ObjectNode detailsJson = OBJECT_MAPPER.createObjectNode();
            detailsJson.put("oldStatus", oldStatus);
            detailsJson.put("newStatus", newStatus);
            
            // Create history record
            WorkflowHistory historyRecord = new WorkflowHistory(workflowInstanceId, entityType, entityId, "STATUS_CHANGE", detailsJson);
            historyRecord.setUsername(username);
            
            return addHistoryRecord(historyRecord);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating status change history record", e);
            throw new SQLException("Error creating status change history record", e);
        }
    }
}
