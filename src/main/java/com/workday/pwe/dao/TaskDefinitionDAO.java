package com.workday.pwe.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workday.pwe.enums.TaskType;
import com.workday.pwe.model.TaskDefinition;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for task_definitions table operations.
 */
public class TaskDefinitionDAO {

    private static final Logger LOGGER = Logger.getLogger(TaskDefinitionDAO.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final Connection connection;
    
    /**
     * Constructor with database connection
     * 
     * @param connection The database connection
     */
    public TaskDefinitionDAO(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.connection = connection;
    }
    
    /**
     * Create a new task definition
     * 
     * @param taskDef The task definition to create
     * @return The ID of the created task definition
     * @throws SQLException If a database error occurs
     */
    public UUID createTaskDefinition(TaskDefinition taskDef) throws SQLException {
        final String sql = "INSERT INTO task_definitions " +
                           "(id, workflow_def_id, task_group_def_id, name, task_type, task_order, parameters_json) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            UUID id = taskDef.getId() != null ? taskDef.getId() : UUID.randomUUID();
            
            stmt.setObject(1, id);
            stmt.setObject(2, taskDef.getWorkflowDefId());
            stmt.setObject(3, taskDef.getTaskGroupDefId()); // Can be null
            stmt.setString(4, taskDef.getName());
            stmt.setString(5, taskDef.getTaskType().name());
            stmt.setInt(6, taskDef.getTaskOrder());
            stmt.setString(7, taskDef.getParametersJson() != null ? taskDef.getParametersJson().toString() : null);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating task definition failed, no rows affected.");
            }
            
            return id;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating task definition", e);
            throw e;
        }
    }
    
    /**
     * Get a task definition by ID
     * 
     * @param id The task definition ID
     * @return The task definition, or null if not found
     * @throws SQLException If a database error occurs
     */
    public TaskDefinition getTaskDefinition(UUID id) throws SQLException {
        final String sql = "SELECT id, workflow_def_id, task_group_def_id, name, task_type, task_order, parameters_json " +
                           "FROM task_definitions WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTaskDefinition(rs);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting task definition", e);
            throw e;
        }
    }
    
    /**
     * Get all task definitions for a workflow
     * 
     * @param workflowDefId The workflow definition ID
     * @return List of task definitions
     * @throws SQLException If a database error occurs
     */
    public List<TaskDefinition> getTaskDefinitionsByWorkflowId(UUID workflowDefId) throws SQLException {
        final String sql = "SELECT id, workflow_def_id, task_group_def_id, name, task_type, task_order, parameters_json " +
                           "FROM task_definitions WHERE workflow_def_id = ? " +
                           "ORDER BY task_order ASC";
        
        List<TaskDefinition> taskDefs = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowDefId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    taskDefs.add(mapResultSetToTaskDefinition(rs));
                }
            }
            
            return taskDefs;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting task definitions by workflow ID", e);
            throw e;
        }
    }
    
    /**
     * Get task definitions for a task group
     * 
     * @param taskGroupDefId The task group definition ID
     * @return List of task definitions
     * @throws SQLException If a database error occurs
     */
    public List<TaskDefinition> getTasksByGroupId(UUID taskGroupDefId) throws SQLException {
        final String sql = "SELECT id, workflow_def_id, task_group_def_id, name, task_type, task_order, parameters_json " +
                           "FROM task_definitions WHERE task_group_def_id = ? " +
                           "ORDER BY task_order ASC";
        
        List<TaskDefinition> taskDefs = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, taskGroupDefId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    taskDefs.add(mapResultSetToTaskDefinition(rs));
                }
            }
            
            return taskDefs;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting task definitions by group ID", e);
            throw e;
        }
    }
    
    /**
     * Get top-level tasks (not in any group) for a workflow
     * 
     * @param workflowDefId The workflow definition ID
     * @return List of top-level task definitions
     * @throws SQLException If a database error occurs
     */
    public List<TaskDefinition> getTopLevelTasks(UUID workflowDefId) throws SQLException {
        final String sql = "SELECT id, workflow_def_id, task_group_def_id, name, task_type, task_order, parameters_json " +
                           "FROM task_definitions WHERE workflow_def_id = ? AND task_group_def_id IS NULL " +
                           "ORDER BY task_order ASC";
        
        List<TaskDefinition> taskDefs = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowDefId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    taskDefs.add(mapResultSetToTaskDefinition(rs));
                }
            }
            
            return taskDefs;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting top-level task definitions", e);
            throw e;
        }
    }
    
    /**
     * Update a task definition
     * 
     * @param taskDef The task definition to update
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int updateTaskDefinition(TaskDefinition taskDef) throws SQLException {
        final String sql = "UPDATE task_definitions SET " +
                           "workflow_def_id = ?, task_group_def_id = ?, name = ?, task_type = ?, " +
                           "task_order = ?, parameters_json = ?::jsonb " +
                           "WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, taskDef.getWorkflowDefId());
            stmt.setObject(2, taskDef.getTaskGroupDefId());
            stmt.setString(3, taskDef.getName());
            stmt.setString(4, taskDef.getTaskType().name());
            stmt.setInt(5, taskDef.getTaskOrder());
            stmt.setString(6, taskDef.getParametersJson() != null ? taskDef.getParametersJson().toString() : null);
            stmt.setObject(7, taskDef.getId());
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating task definition", e);
            throw e;
        }
    }
    
    /**
     * Delete a task definition
     * 
     * @param id The task definition ID
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int deleteTaskDefinition(UUID id) throws SQLException {
        final String sql = "DELETE FROM task_definitions WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting task definition", e);
            throw e;
        }
    }
    
    /**
     * Map a result set row to a TaskDefinition object
     * 
     * @param rs The result set
     * @return The mapped TaskDefinition
     * @throws SQLException If a database error occurs
     */
    private TaskDefinition mapResultSetToTaskDefinition(ResultSet rs) throws SQLException {
        TaskDefinition taskDef = new TaskDefinition();
        
        taskDef.setId(UUID.fromString(rs.getString("id")));
        taskDef.setWorkflowDefId(UUID.fromString(rs.getString("workflow_def_id")));
        
        String taskGroupDefId = rs.getString("task_group_def_id");
        if (taskGroupDefId != null) {
            taskDef.setTaskGroupDefId(UUID.fromString(taskGroupDefId));
        }
        
        taskDef.setName(rs.getString("name"));
        taskDef.setTaskType(TaskType.valueOf(rs.getString("task_type")));
        taskDef.setTaskOrder(rs.getInt("task_order"));
        
        // Parse the JSON parameters
        try {
            String jsonStr = rs.getString("parameters_json");
            if (jsonStr != null) {
                JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonStr);
                taskDef.setParametersJson(jsonNode);
            }
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Error parsing parameters JSON", e);
            throw new SQLException("Error parsing parameters JSON", e);
        }
        
        return taskDef;
    }
    
    /**
     * Get tasks of a specific type for a workflow
     * 
     * @param workflowDefId The workflow definition ID
     * @param taskType The task type
     * @return List of task definitions of the specified type
     * @throws SQLException If a database error occurs
     */
    public List<TaskDefinition> getTasksByType(UUID workflowDefId, TaskType taskType) throws SQLException {
        final String sql = "SELECT id, workflow_def_id, task_group_def_id, name, task_type, task_order, parameters_json " +
                           "FROM task_definitions WHERE workflow_def_id = ? AND task_type = ? " +
                           "ORDER BY task_order ASC";
        
        List<TaskDefinition> taskDefs = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowDefId);
            stmt.setString(2, taskType.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    taskDefs.add(mapResultSetToTaskDefinition(rs));
                }
            }
            
            return taskDefs;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting task definitions by type", e);
            throw e;
        }
    }
    
    /**
     * Check if a task definition exists
     * 
     * @param id The task definition ID
     * @return true if the task definition exists, false otherwise
     * @throws SQLException If a database error occurs
     */
    public boolean taskDefinitionExists(UUID id) throws SQLException {
        final String sql = "SELECT 1 FROM task_definitions WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if task definition exists", e);
            throw e;
        }
    }
}