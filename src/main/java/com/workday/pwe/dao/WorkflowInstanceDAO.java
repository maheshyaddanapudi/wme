package com.workday.pwe.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workday.pwe.enums.WorkflowStatus;
import com.workday.pwe.model.WorkflowInstance;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for workflow_instances table operations.
 */
public class WorkflowInstanceDAO {

    private static final Logger LOGGER = Logger.getLogger(WorkflowInstanceDAO.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final Connection connection;
    
    /**
     * Constructor with database connection
     * 
     * @param connection The database connection
     */
    public WorkflowInstanceDAO(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.connection = connection;
    }
    
    /**
     * Create a new workflow instance
     * 
     * @param workflowInst The workflow instance to create
     * @return The ID of the created workflow instance
     * @throws SQLException If a database error occurs
     */
    public UUID createWorkflowInstance(WorkflowInstance workflowInst) throws SQLException {
        final String sql = "INSERT INTO workflow_instances " +
                           "(id, workflow_def_id, status, input_json, output_json, start_time, end_time, created_at, updated_at) " +
                           "VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            UUID id = workflowInst.getId() != null ? workflowInst.getId() : UUID.randomUUID();
            
            stmt.setObject(1, id);
            stmt.setObject(2, workflowInst.getWorkflowDefId());
            stmt.setString(3, workflowInst.getStatus().name());
            stmt.setString(4, workflowInst.getInputJson() != null ? workflowInst.getInputJson().toString() : null);
            stmt.setString(5, workflowInst.getOutputJson() != null ? workflowInst.getOutputJson().toString() : null);
            stmt.setTimestamp(6, workflowInst.getStartTime() != null ? Timestamp.valueOf(workflowInst.getStartTime()) : null);
            stmt.setTimestamp(7, workflowInst.getEndTime() != null ? Timestamp.valueOf(workflowInst.getEndTime()) : null);
            stmt.setTimestamp(8, Timestamp.valueOf(workflowInst.getCreatedAt() != null ? 
                                                 workflowInst.getCreatedAt() : LocalDateTime.now()));
            stmt.setTimestamp(9, Timestamp.valueOf(workflowInst.getUpdatedAt() != null ? 
                                                 workflowInst.getUpdatedAt() : LocalDateTime.now()));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating workflow instance failed, no rows affected.");
            }
            
            return id;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating workflow instance", e);
            throw e;
        }
    }
    
    /**
     * Get a workflow instance by ID
     * 
     * @param id The workflow instance ID
     * @return The workflow instance, or null if not found
     * @throws SQLException If a database error occurs
     */
    public WorkflowInstance getWorkflowInstance(UUID id) throws SQLException {
        final String sql = "SELECT id, workflow_def_id, status, input_json, output_json, start_time, end_time, created_at, updated_at " +
                           "FROM workflow_instances WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToWorkflowInstance(rs);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting workflow instance", e);
            throw e;
        }
    }
    
    /**
     * Get all workflow instances for a workflow definition
     * 
     * @param workflowDefId The workflow definition ID
     * @return List of workflow instances
     * @throws SQLException If a database error occurs
     */
    public List<WorkflowInstance> getWorkflowInstancesByDefinitionId(UUID workflowDefId) throws SQLException {
        final String sql = "SELECT id, workflow_def_id, status, input_json, output_json, start_time, end_time, created_at, updated_at " +
                           "FROM workflow_instances WHERE workflow_def_id = ?";
        
        List<WorkflowInstance> instances = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowDefId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    instances.add(mapResultSetToWorkflowInstance(rs));
                }
            }
            
            return instances;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting workflow instances by definition ID", e);
            throw e;
        }
    }
    
    /**
     * Get workflow instances by status
     * 
     * @param status The workflow status
     * @return List of workflow instances with the given status
     * @throws SQLException If a database error occurs
     */
    public List<WorkflowInstance> getWorkflowInstancesByStatus(WorkflowStatus status) throws SQLException {
        final String sql = "SELECT id, workflow_def_id, status, input_json, output_json, start_time, end_time, created_at, updated_at " +
                           "FROM workflow_instances WHERE status = ?";
        
        List<WorkflowInstance> instances = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    instances.add(mapResultSetToWorkflowInstance(rs));
                }
            }
            
            return instances;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting workflow instances by status", e);
            throw e;
        }
    }
    
    /**
     * Update a workflow instance
     * 
     * @param workflowInst The workflow instance to update
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int updateWorkflowInstance(WorkflowInstance workflowInst) throws SQLException {
        final String sql = "UPDATE workflow_instances SET " +
                           "workflow_def_id = ?, status = ?, input_json = ?::jsonb, output_json = ?::jsonb, " +
                           "start_time = ?, end_time = ?, updated_at = ? " +
                           "WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowInst.getWorkflowDefId());
            stmt.setString(2, workflowInst.getStatus().name());
            stmt.setString(3, workflowInst.getInputJson() != null ? workflowInst.getInputJson().toString() : null);
            stmt.setString(4, workflowInst.getOutputJson() != null ? workflowInst.getOutputJson().toString() : null);
            stmt.setTimestamp(5, workflowInst.getStartTime() != null ? Timestamp.valueOf(workflowInst.getStartTime()) : null);
            stmt.setTimestamp(6, workflowInst.getEndTime() != null ? Timestamp.valueOf(workflowInst.getEndTime()) : null);
            stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setObject(8, workflowInst.getId());
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating workflow instance", e);
            throw e;
        }
    }
    
    /**
     * Update a workflow instance's status
     * 
     * @param id The workflow instance ID
     * @param status The new status
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int updateWorkflowStatus(UUID id, WorkflowStatus status) throws SQLException {
        final String sql = "UPDATE workflow_instances SET status = ?, updated_at = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setObject(3, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating workflow status", e);
            throw e;
        }
    }
    
    /**
     * Update workflow output and status
     * 
     * @param id The workflow instance ID
     * @param outputJson The output data
     * @param status The new status
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int updateOutputAndStatus(UUID id, JsonNode outputJson, WorkflowStatus status) throws SQLException {
        final String sql = "UPDATE workflow_instances SET output_json = ?::jsonb, status = ?, updated_at = ?, end_time = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, outputJson != null ? outputJson.toString() : null);
            stmt.setString(2, status.name());
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setObject(5, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating workflow output and status", e);
            throw e;
        }
    }
    
    /**
     * Delete a workflow instance
     * 
     * @param id The workflow instance ID
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int deleteWorkflowInstance(UUID id) throws SQLException {
        final String sql = "DELETE FROM workflow_instances WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting workflow instance", e);
            throw e;
        }
    }
    
    /**
     * Map a result set row to a WorkflowInstance object
     * 
     * @param rs The result set
     * @return The mapped WorkflowInstance
     * @throws SQLException If a database error occurs
     */
    private WorkflowInstance mapResultSetToWorkflowInstance(ResultSet rs) throws SQLException {
        WorkflowInstance workflowInst = new WorkflowInstance();
        
        workflowInst.setId(UUID.fromString(rs.getString("id")));
        workflowInst.setWorkflowDefId(UUID.fromString(rs.getString("workflow_def_id")));
        workflowInst.setStatus(WorkflowStatus.valueOf(rs.getString("status")));
        workflowInst.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        workflowInst.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        
        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            workflowInst.setStartTime(startTime.toLocalDateTime());
        }
        
        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            workflowInst.setEndTime(endTime.toLocalDateTime());
        }
        
        // Parse the JSON data
        try {
            String inputJsonStr = rs.getString("input_json");
            if (inputJsonStr != null) {
                JsonNode inputJson = OBJECT_MAPPER.readTree(inputJsonStr);
                workflowInst.setInputJson(inputJson);
            }
            
            String outputJsonStr = rs.getString("output_json");
            if (outputJsonStr != null) {
                JsonNode outputJson = OBJECT_MAPPER.readTree(outputJsonStr);
                workflowInst.setOutputJson(outputJson);
            }
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Error parsing JSON data", e);
            throw new SQLException("Error parsing JSON data", e);
        }
        
        return workflowInst;
    }
    
    /**
     * Check if a workflow is in progress
     * 
     * @param id The workflow instance ID
     * @return true if the workflow is in progress, false otherwise
     * @throws SQLException If a database error occurs
     */
    public boolean isWorkflowInProgress(UUID id) throws SQLException {
        final String sql = "SELECT 1 FROM workflow_instances WHERE id = ? AND status IN ('RUNNING', 'PAUSED')";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if workflow is in progress", e);
            throw e;
        }
    }
    
    /**
     * Archive completed workflows older than a certain date
     * 
     * @param olderThan The date before which to archive workflows
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int archiveOldWorkflows(LocalDateTime olderThan) throws SQLException {
        final String sql = "UPDATE workflow_instances SET status = 'ARCHIVED' " +
                           "WHERE status IN ('COMPLETED', 'FAILED', 'TERMINATED') " +
                           "AND end_time < ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(olderThan));
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error archiving old workflows", e);
            throw e;
        }
    }
}