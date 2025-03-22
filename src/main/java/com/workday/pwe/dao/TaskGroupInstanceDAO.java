package com.workday.pwe.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.model.TaskGroupDefinition;
import com.workday.pwe.model.TaskGroupInstance;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for task_group_instances table operations.
 */
public class TaskGroupInstanceDAO {

    private static final Logger LOGGER = Logger.getLogger(TaskGroupInstanceDAO.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final Connection connection;
    
    /**
     * Constructor with database connection
     * 
     * @param connection The database connection
     */
    public TaskGroupInstanceDAO(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.connection = connection;
    }
    
    /**
     * Create a new task group instance
     * 
     * @param groupInst The task group instance to create
     * @return The ID of the created task group instance
     * @throws SQLException If a database error occurs
     */
    public UUID createTaskGroupInstance(TaskGroupInstance groupInst) throws SQLException {
        final String sql = "INSERT INTO task_group_instances " +
                           "(id, workflow_instance_id, task_group_def_id, parent_group_inst_id, status, " +
                           "min_completion, parameters_json, start_time, end_time) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            UUID id = groupInst.getId() != null ? groupInst.getId() : UUID.randomUUID();
            
            stmt.setObject(1, id);
            stmt.setObject(2, groupInst.getWorkflowInstanceId());
            stmt.setObject(3, groupInst.getTaskGroupDefId());
            stmt.setObject(4, groupInst.getParentGroupInstId()); // Can be null
            stmt.setString(5, groupInst.getStatus().name());
            stmt.setInt(6, groupInst.getMinCompletion());
            stmt.setString(7, groupInst.getParametersJson() != null ? groupInst.getParametersJson().toString() : null);
            stmt.setTimestamp(8, groupInst.getStartTime() != null ? Timestamp.valueOf(groupInst.getStartTime()) : null);
            stmt.setTimestamp(9, groupInst.getEndTime() != null ? Timestamp.valueOf(groupInst.getEndTime()) : null);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating task group instance failed, no rows affected.");
            }
            
            return id;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating task group instance", e);
            throw e;
        }
    }
    
    /**
     * Get a task group instance by ID
     * 
     * @param id The task group instance ID
     * @return The task group instance, or null if not found
     * @throws SQLException If a database error occurs
     */
    public TaskGroupInstance getTaskGroupInstance(UUID id) throws SQLException {
        final String sql = "SELECT id, workflow_instance_id, task_group_def_id, parent_group_inst_id, status, " +
                           "min_completion, parameters_json, start_time, end_time " +
                           "FROM task_group_instances WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTaskGroupInstance(rs);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting task group instance", e);
            throw e;
        }
    }
    
    /**
     * Get task group instances for a workflow
     * 
     * @param workflowInstanceId The workflow instance ID
     * @return List of task group instances
     * @throws SQLException If a database error occurs
     */
    public List<TaskGroupInstance> getTaskGroupsByWorkflowId(UUID workflowInstanceId) throws SQLException {
        final String sql = "SELECT tgi.id, tgi.workflow_instance_id, tgi.task_group_def_id, tgi.parent_group_inst_id, " +
                           "tgi.status, tgi.min_completion, tgi.parameters_json, tgi.start_time, tgi.end_time " +
                           "FROM task_group_instances tgi " +
                           "JOIN task_group_definitions tgd ON tgi.task_group_def_id = tgd.id " +
                           "WHERE tgi.workflow_instance_id = ? " +
                           "ORDER BY tgd.group_order ASC";
        
        List<TaskGroupInstance> groupInsts = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowInstanceId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    groupInsts.add(mapResultSetToTaskGroupInstance(rs));
                }
            }
            
            return groupInsts;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting task group instances by workflow ID", e);
            throw e;
        }
    }
    
    /**
     * Get child task group instances for a parent group
     * 
     * @param parentGroupInstId The parent task group instance ID
     * @return List of child task group instances
     * @throws SQLException If a database error occurs
     */
    public List<TaskGroupInstance> getChildGroups(UUID parentGroupInstId) throws SQLException {
        final String sql = "SELECT tgi.id, tgi.workflow_instance_id, tgi.task_group_def_id, tgi.parent_group_inst_id, " +
                           "tgi.status, tgi.min_completion, tgi.parameters_json, tgi.start_time, tgi.end_time " +
                           "FROM task_group_instances tgi " +
                           "JOIN task_group_definitions tgd ON tgi.task_group_def_id = tgd.id " +
                           "WHERE tgi.parent_group_inst_id = ? " +
                           "ORDER BY tgd.group_order ASC";
        
        List<TaskGroupInstance> groupInsts = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, parentGroupInstId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    groupInsts.add(mapResultSetToTaskGroupInstance(rs));
                }
            }
            
            return groupInsts;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting child task group instances", e);
            throw e;
        }
    }
    
    /**
     * Get root task group instances (not in any parent group) for a workflow
     * 
     * @param workflowInstanceId The workflow instance ID
     * @return List of root task group instances
     * @throws SQLException If a database error occurs
     */
    public List<TaskGroupInstance> getRootTaskGroups(UUID workflowInstanceId) throws SQLException {
        final String sql = "SELECT tgi.id, tgi.workflow_instance_id, tgi.task_group_def_id, tgi.parent_group_inst_id, " +
                           "tgi.status, tgi.min_completion, tgi.parameters_json, tgi.start_time, tgi.end_time " +
                           "FROM task_group_instances tgi " +
                           "JOIN task_group_definitions tgd ON tgi.task_group_def_id = tgd.id " +
                           "WHERE tgi.workflow_instance_id = ? AND tgi.parent_group_inst_id IS NULL " +
                           "ORDER BY tgd.group_order ASC";
        
        List<TaskGroupInstance> groupInsts = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowInstanceId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    groupInsts.add(mapResultSetToTaskGroupInstance(rs));
                }
            }
            
            return groupInsts;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting root task group instances", e);
            throw e;
        }
    }
    
    /**
     * Update a task group instance
     * 
     * @param groupInst The task group instance to update
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int updateTaskGroupInstance(TaskGroupInstance groupInst) throws SQLException {
        final String sql = "UPDATE task_group_instances SET " +
                           "workflow_instance_id = ?, task_group_def_id = ?, parent_group_inst_id = ?, " +
                           "status = ?, min_completion = ?, parameters_json = ?::jsonb, start_time = ?, end_time = ? " +
                           "WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, groupInst.getWorkflowInstanceId());
            stmt.setObject(2, groupInst.getTaskGroupDefId());
            stmt.setObject(3, groupInst.getParentGroupInstId());
            stmt.setString(4, groupInst.getStatus().name());
            stmt.setInt(5, groupInst.getMinCompletion());
            stmt.setString(6, groupInst.getParametersJson() != null ? groupInst.getParametersJson().toString() : null);
            stmt.setTimestamp(7, groupInst.getStartTime() != null ? Timestamp.valueOf(groupInst.getStartTime()) : null);
            stmt.setTimestamp(8, groupInst.getEndTime() != null ? Timestamp.valueOf(groupInst.getEndTime()) : null);
            stmt.setObject(9, groupInst.getId());
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating task group instance", e);
            throw e;
        }
    }
    
    /**
     * Update a task group instance's status
     * 
     * @param id The task group instance ID
     * @param status The new status
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int updateTaskGroupStatus(UUID id, TaskStatus status) throws SQLException {
        final String sql = "UPDATE task_group_instances SET status = ?, " +
                           "end_time = CASE WHEN ? IN ('COMPLETED', 'FAILED', 'SKIPPED') AND end_time IS NULL THEN ? ELSE end_time END " +
                           "WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, status.name());
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setObject(4, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating task group status", e);
            throw e;
        }
    }
    
    /**
     * Update a task group instance with failure reason
     * 
     * @param id The task group instance ID
     * @param status The new status
     * @param failureReason The reason for failure
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int updateTaskGroupInstance(TaskGroupInstance groupInst, String failureReason) throws SQLException {
        // We need to store the failure reason in parameters_json since there's no dedicated column
        try {
            JsonNode parametersJson = groupInst.getParametersJson();
            if (parametersJson == null) {
                parametersJson = OBJECT_MAPPER.createObjectNode();
            }
            
            // Add failure reason to parameters JSON
            ((com.fasterxml.jackson.databind.node.ObjectNode) parametersJson).put("failureReason", failureReason);
            groupInst.setParametersJson(parametersJson);
            
            return updateTaskGroupInstance(groupInst);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding failure reason to task group instance", e);
            throw new SQLException("Error adding failure reason to task group instance", e);
        }
    }
    
    /**
     * Delete a task group instance
     * 
     * @param id The task group instance ID
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int deleteTaskGroupInstance(UUID id) throws SQLException {
        final String sql = "DELETE FROM task_group_instances WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting task group instance", e);
            throw e;
        }
    }
    
    /**
     * Get completed task groups for a workflow
     * 
     * @param workflowInstanceId The workflow instance ID
     * @return List of completed task group instances
     * @throws SQLException If a database error occurs
     */
    public List<TaskGroupInstance> getCompletedGroups(UUID workflowInstanceId) throws SQLException {
        final String sql = "SELECT id, workflow_instance_id, task_group_def_id, parent_group_inst_id, status, " +
                           "min_completion, parameters_json, start_time, end_time " +
                           "FROM task_group_instances " +
                           "WHERE workflow_instance_id = ? AND status IN ('COMPLETED', 'FAILED', 'SKIPPED')";
        
        List<TaskGroupInstance> groupInsts = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowInstanceId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    groupInsts.add(mapResultSetToTaskGroupInstance(rs));
                }
            }
            
            return groupInsts;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting completed task groups", e);
            throw e;
        }
    }
    
    /**
     * Get completed task groups for a workflow after a certain time
     * 
     * @param workflowInstanceId The workflow instance ID
     * @param afterTimestamp Task groups completed after this timestamp
     * @return List of completed task group instances
     * @throws SQLException If a database error occurs
     */
    public List<TaskGroupInstance> getCompletedGroupsAfter(UUID workflowInstanceId, Timestamp afterTimestamp) throws SQLException {
        final String sql = "SELECT id, workflow_instance_id, task_group_def_id, parent_group_inst_id, status, " +
                           "min_completion, parameters_json, start_time, end_time " +
                           "FROM task_group_instances " +
                           "WHERE workflow_instance_id = ? AND status IN ('COMPLETED', 'FAILED', 'SKIPPED') " +
                           "AND end_time > ?";
        
        List<TaskGroupInstance> groupInsts = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowInstanceId);
            stmt.setTimestamp(2, afterTimestamp);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    groupInsts.add(mapResultSetToTaskGroupInstance(rs));
                }
            }
            
            return groupInsts;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting completed task groups after timestamp", e);
            throw e;
        }
    }
    
    /**
     * Get the task group definition for a task group instance
     * 
     * @param taskGroupDefId The task group definition ID
     * @return The task group definition, or null if not found
     * @throws SQLException If a database error occurs
     */
    public TaskGroupDefinition getTaskGroupDefinition(UUID taskGroupDefId) throws SQLException {
        return new TaskGroupDefinitionDAO(connection).getTaskGroupDefinition(taskGroupDefId);
    }
    
    /**
     * Map a result set row to a TaskGroupInstance object
     * 
     * @param rs The result set
     * @return The mapped TaskGroupInstance
     * @throws SQLException If a database error occurs
     */
    private TaskGroupInstance mapResultSetToTaskGroupInstance(ResultSet rs) throws SQLException {
        TaskGroupInstance groupInst = new TaskGroupInstance();
        
        groupInst.setId(UUID.fromString(rs.getString("id")));
        groupInst.setWorkflowInstanceId(UUID.fromString(rs.getString("workflow_instance_id")));
        groupInst.setTaskGroupDefId(UUID.fromString(rs.getString("task_group_def_id")));
        
        String parentGroupInstId = rs.getString("parent_group_inst_id");
        if (parentGroupInstId != null) {
            groupInst.setParentGroupInstId(UUID.fromString(parentGroupInstId));
        }
        
        groupInst.setStatus(TaskStatus.valueOf(rs.getString("status")));
        groupInst.setMinCompletion(rs.getInt("min_completion"));
        
        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            groupInst.setStartTime(startTime.toLocalDateTime());
        }
        
        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            groupInst.setEndTime(endTime.toLocalDateTime());
        }
        
        // Parse the JSON parameters
        try {
            String jsonStr = rs.getString("parameters_json");
            if (jsonStr != null) {
                JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonStr);
                groupInst.setParametersJson(jsonNode);
            }
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Error parsing parameters JSON", e);
            throw new SQLException("Error parsing parameters JSON", e);
        }
        
        return groupInst;
    }
    
    /**
     * Check if a vertical parent group has any child in progress
     * 
     * @param parentGroupInstId The parent task group instance ID
     * @return true if any child is in progress, false otherwise
     * @throws SQLException If a database error occurs
     */
    public boolean hasInProgressChildren(UUID parentGroupInstId) throws SQLException {
        final String sql = "SELECT 1 FROM task_group_instances " +
                           "WHERE parent_group_inst_id = ? AND status IN ('NOT_STARTED', 'IN_PROGRESS', 'BLOCKED')";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, parentGroupInstId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if group has in-progress children", e);
            throw e;
        }
    }
    
    /**
     * Get task group instances by status
     * 
     * @param workflowInstanceId The workflow instance ID
     * @param status The task group status
     * @return List of task group instances with the specified status
     * @throws SQLException If a database error occurs
     */
    public List<TaskGroupInstance> getTaskGroupsByStatus(UUID workflowInstanceId, TaskStatus status) throws SQLException {
        final String sql = "SELECT id, workflow_instance_id, task_group_def_id, parent_group_inst_id, status, " +
                           "min_completion, parameters_json, start_time, end_time " +
                           "FROM task_group_instances " +
                           "WHERE workflow_instance_id = ? AND status = ?";
        
        List<TaskGroupInstance> groupInsts = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowInstanceId);
            stmt.setString(2, status.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    groupInsts.add(mapResultSetToTaskGroupInstance(rs));
                }
            }
            
            return groupInsts;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting task groups by status", e);
            throw e;
        }
    }
}