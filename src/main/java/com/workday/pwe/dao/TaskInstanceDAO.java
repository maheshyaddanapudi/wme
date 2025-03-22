package com.workday.pwe.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.model.TaskDefinition;
import com.workday.pwe.model.TaskInstance;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for task_instances table operations.
 */
public class TaskInstanceDAO {

    private static final Logger LOGGER = Logger.getLogger(TaskInstanceDAO.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final Connection connection;
    
    /**
     * Constructor with database connection
     * 
     * @param connection The database connection
     */
    public TaskInstanceDAO(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.connection = connection;
    }
    
    /**
     * Create a new task instance
     * 
     * @param taskInst The task instance to create
     * @return The ID of the created task instance
     * @throws SQLException If a database error occurs
     */
    public UUID createTaskInstance(TaskInstance taskInst) throws SQLException {
        final String sql = "INSERT INTO task_instances " +
                           "(id, workflow_instance_id, task_def_id, task_group_instance_id, assignee, status, " +
                           "input_json, output_json, start_time, end_time, due_date, failure_reason) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            UUID id = taskInst.getId() != null ? taskInst.getId() : UUID.randomUUID();
            
            stmt.setObject(1, id);
            stmt.setObject(2, taskInst.getWorkflowInstanceId());
            stmt.setObject(3, taskInst.getTaskDefId());
            stmt.setObject(4, taskInst.getTaskGroupInstanceId()); // Can be null
            stmt.setString(5, taskInst.getAssignee());
            stmt.setString(6, taskInst.getStatus().name());
            stmt.setString(7, taskInst.getInputJson() != null ? taskInst.getInputJson().toString() : null);
            stmt.setString(8, taskInst.getOutputJson() != null ? taskInst.getOutputJson().toString() : null);
            stmt.setTimestamp(9, taskInst.getStartTime() != null ? Timestamp.valueOf(taskInst.getStartTime()) : null);
            stmt.setTimestamp(10, taskInst.getEndTime() != null ? Timestamp.valueOf(taskInst.getEndTime()) : null);
            stmt.setTimestamp(11, taskInst.getDueDate() != null ? Timestamp.valueOf(taskInst.getDueDate()) : null);
            stmt.setString(12, taskInst.getFailureReason());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating task instance failed, no rows affected.");
            }
            
            return id;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating task instance", e);
            throw e;
        }
    }
    
    /**
     * Get a task instance by ID
     * 
     * @param id The task instance ID
     * @return The task instance, or null if not found
     * @throws SQLException If a database error occurs
     */
    public TaskInstance getTaskInstance(UUID id) throws SQLException {
        final String sql = "SELECT id, workflow_instance_id, task_def_id, task_group_instance_id, assignee, status, " +
                           "input_json, output_json, start_time, end_time, due_date, failure_reason " +
                           "FROM task_instances WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTaskInstance(rs);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting task instance", e);
            throw e;
        }
    }
    
    /**
     * Get task instances for a workflow
     * 
     * @param workflowInstanceId The workflow instance ID
     * @return List of task instances
     * @throws SQLException If a database error occurs
     */
    public List<TaskInstance> getTaskInstancesByWorkflowId(UUID workflowInstanceId) throws SQLException {
        final String sql = "SELECT ti.id, ti.workflow_instance_id, ti.task_def_id, ti.task_group_instance_id, " +
                           "ti.assignee, ti.status, ti.input_json, ti.output_json, ti.start_time, ti.end_time, " +
                           "ti.due_date, ti.failure_reason " +
                           "FROM task_instances ti " +
                           "JOIN task_definitions td ON ti.task_def_id = td.id " +
                           "WHERE ti.workflow_instance_id = ? " +
                           "ORDER BY td.task_order ASC";
        
        List<TaskInstance> taskInsts = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowInstanceId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    taskInsts.add(mapResultSetToTaskInstance(rs));
                }
            }
            
            return taskInsts;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting task instances by workflow ID", e);
            throw e;
        }
    }
    
    /**
     * Get task instances for a task group
     * 
     * @param taskGroupInstanceId The task group instance ID
     * @return List of task instances
     * @throws SQLException If a database error occurs
     */
    public List<TaskInstance> getTaskInstancesByGroupId(UUID taskGroupInstanceId) throws SQLException {
        final String sql = "SELECT ti.id, ti.workflow_instance_id, ti.task_def_id, ti.task_group_instance_id, " +
                           "ti.assignee, ti.status, ti.input_json, ti.output_json, ti.start_time, ti.end_time, " +
                           "ti.due_date, ti.failure_reason " +
                           "FROM task_instances ti " +
                           "JOIN task_definitions td ON ti.task_def_id = td.id " +
                           "WHERE ti.task_group_instance_id = ? " +
                           "ORDER BY td.task_order ASC";
        
        List<TaskInstance> taskInsts = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, taskGroupInstanceId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    taskInsts.add(mapResultSetToTaskInstance(rs));
                }
            }
            
            return taskInsts;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting task instances by group ID", e);
            throw e;
        }
    }
    
    /**
     * Get top-level task instances (not in any group) for a workflow
     * 
     * @param workflowInstanceId The workflow instance ID
     * @return List of top-level task instances
     * @throws SQLException If a database error occurs
     */
    public List<TaskInstance> getTopLevelTasks(UUID workflowInstanceId) throws SQLException {
        final String sql = "SELECT ti.id, ti.workflow_instance_id, ti.task_def_id, ti.task_group_instance_id, " +
                           "ti.assignee, ti.status, ti.input_json, ti.output_json, ti.start_time, ti.end_time, " +
                           "ti.due_date, ti.failure_reason " +
                           "FROM task_instances ti " +
                           "JOIN task_definitions td ON ti.task_def_id = td.id " +
                           "WHERE ti.workflow_instance_id = ? AND ti.task_group_instance_id IS NULL " +
                           "ORDER BY td.task_order ASC";
        
        List<TaskInstance> taskInsts = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowInstanceId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    taskInsts.add(mapResultSetToTaskInstance(rs));
                }
            }
            
            return taskInsts;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting top-level task instances", e);
            throw e;
        }
    }
    
    /**
     * Update a task instance
     * 
     * @param taskInst The task instance to update
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int updateTaskInstance(TaskInstance taskInst) throws SQLException {
        final String sql = "UPDATE task_instances SET " +
                           "workflow_instance_id = ?, task_def_id = ?, task_group_instance_id = ?, assignee = ?, " +
                           "status = ?, input_json = ?::jsonb, output_json = ?::jsonb, start_time = ?, end_time = ?, " +
                           "due_date = ?, failure_reason = ? " +
                           "WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, taskInst.getWorkflowInstanceId());
            stmt.setObject(2, taskInst.getTaskDefId());
            stmt.setObject(3, taskInst.getTaskGroupInstanceId());
            stmt.setString(4, taskInst.getAssignee());
            stmt.setString(5, taskInst.getStatus().name());
            stmt.setString(6, taskInst.getInputJson() != null ? taskInst.getInputJson().toString() : null);
            stmt.setString(7, taskInst.getOutputJson() != null ? taskInst.getOutputJson().toString() : null);
            stmt.setTimestamp(8, taskInst.getStartTime() != null ? Timestamp.valueOf(taskInst.getStartTime()) : null);
            stmt.setTimestamp(9, taskInst.getEndTime() != null ? Timestamp.valueOf(taskInst.getEndTime()) : null);
            stmt.setTimestamp(10, taskInst.getDueDate() != null ? Timestamp.valueOf(taskInst.getDueDate()) : null);
            stmt.setString(11, taskInst.getFailureReason());
            stmt.setObject(12, taskInst.getId());
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating task instance", e);
            throw e;
        }
    }
    
    /**
     * Update a task instance's status
     * 
     * @param id The task instance ID
     * @param status The new status
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int updateTask(UUID id, TaskStatus status) throws SQLException {
        final String sql = "UPDATE task_instances SET status = ? " +
                           "WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setObject(2, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating task status", e);
            throw e;
        }
    }
    
    /**
     * Update a task instance's status and failure reason
     * 
     * @param id The task instance ID
     * @param status The new status
     * @param failureReason The reason for failure
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int updateTask(UUID id, TaskStatus status, String failureReason) throws SQLException {
        final String sql = "UPDATE task_instances SET status = ?, failure_reason = ? " +
                           "WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, failureReason);
            stmt.setObject(3, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating task status and failure reason", e);
            throw e;
        }
    }
    
    /**
     * Update a task instance's output and status
     * 
     * @param id The task instance ID
     * @param outputJson The output data
     * @param status The new status
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int updateOutputAndStatus(UUID id, JsonNode outputJson, TaskStatus status) throws SQLException {
        final String sql = "UPDATE task_instances SET output_json = ?::jsonb, status = ?, end_time = ? " +
                           "WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, outputJson != null ? outputJson.toString() : null);
            stmt.setString(2, status.name());
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setObject(4, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating task output and status", e);
            throw e;
        }
    }
    
    /**
     * Delete a task instance
     * 
     * @param id The task instance ID
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int deleteTaskInstance(UUID id) throws SQLException {
        final String sql = "DELETE FROM task_instances WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting task instance", e);
            throw e;
        }
    }
    
    /**
     * Get completed tasks for a workflow
     * 
     * @param workflowInstanceId The workflow instance ID
     * @return List of completed task instances
     * @throws SQLException If a database error occurs
     */
    public List<TaskInstance> getCompletedTasks(UUID workflowInstanceId) throws SQLException {
        final String sql = "SELECT id, workflow_instance_id, task_def_id, task_group_instance_id, assignee, status, " +
                           "input_json, output_json, start_time, end_time, due_date, failure_reason " +
                           "FROM task_instances " +
                           "WHERE workflow_instance_id = ? AND status IN " +
                           "('COMPLETED', 'SUBMITTED', 'APPROVED', 'REVIEWED', 'API_CALL_COMPLETE', 'SKIPPED', 'FAILED', 'EXPIRED')";
        
        List<TaskInstance> taskInsts = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowInstanceId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    taskInsts.add(mapResultSetToTaskInstance(rs));
                }
            }
            
            return taskInsts;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting completed tasks", e);
            throw e;
        }
    }
    
    /**
     * Get completed tasks for a workflow after a certain time
     * 
     * @param workflowInstanceId The workflow instance ID
     * @param afterTimestamp Tasks completed after this timestamp
     * @return List of completed task instances
     * @throws SQLException If a database error occurs
     */
    public List<TaskInstance> getCompletedTasksAfter(UUID workflowInstanceId, Timestamp afterTimestamp) throws SQLException {
        final String sql = "SELECT id, workflow_instance_id, task_def_id, task_group_instance_id, assignee, status, " +
                           "input_json, output_json, start_time, end_time, due_date, failure_reason " +
                           "FROM task_instances " +
                           "WHERE workflow_instance_id = ? AND status IN " +
                           "('COMPLETED', 'SUBMITTED', 'APPROVED', 'REVIEWED', 'API_CALL_COMPLETE', 'SKIPPED', 'FAILED', 'EXPIRED') " +
                           "AND end_time > ?";
        
        List<TaskInstance> taskInsts = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowInstanceId);
            stmt.setTimestamp(2, afterTimestamp);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    taskInsts.add(mapResultSetToTaskInstance(rs));
                }
            }
            
            return taskInsts;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting completed tasks after timestamp", e);
            throw e;
        }
    }
    
    /**
     * Get the task definition for a task instance
     * 
     * @param taskDefId The task definition ID
     * @return The task definition, or null if not found
     * @throws SQLException If a database error occurs
     */
    public TaskDefinition getTaskDefinition(UUID taskDefId) throws SQLException {
        return new TaskDefinitionDAO(connection).getTaskDefinition(taskDefId);
    }
    
    /**
     * Map a result set row to a TaskInstance object
     * 
     * @param rs The result set
     * @return The mapped TaskInstance
     * @throws SQLException If a database error occurs
     */
    private TaskInstance mapResultSetToTaskInstance(ResultSet rs) throws SQLException {
        TaskInstance taskInst = new TaskInstance();
        
        taskInst.setId(UUID.fromString(rs.getString("id")));
        taskInst.setWorkflowInstanceId(UUID.fromString(rs.getString("workflow_instance_id")));
        taskInst.setTaskDefId(UUID.fromString(rs.getString("task_def_id")));
        
        String taskGroupInstanceId = rs.getString("task_group_instance_id");
        if (taskGroupInstanceId != null) {
            taskInst.setTaskGroupInstanceId(UUID.fromString(taskGroupInstanceId));
        }
        
        taskInst.setAssignee(rs.getString("assignee"));
        taskInst.setStatus(TaskStatus.valueOf(rs.getString("status")));
        taskInst.setFailureReason(rs.getString("failure_reason"));
        
        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            taskInst.setStartTime(startTime.toLocalDateTime());
        }
        
        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            taskInst.setEndTime(endTime.toLocalDateTime());
        }
        
        Timestamp dueDate = rs.getTimestamp("due_date");
        if (dueDate != null) {
            taskInst.setDueDate(dueDate.toLocalDateTime());
        }
        
        // Parse the JSON data
        try {
            String inputJsonStr = rs.getString("input_json");
            if (inputJsonStr != null) {
                JsonNode inputJson = OBJECT_MAPPER.readTree(inputJsonStr);
                taskInst.setInputJson(inputJson);
            }
            
            String outputJsonStr = rs.getString("output_json");
            if (outputJsonStr != null) {
                JsonNode outputJson = OBJECT_MAPPER.readTree(outputJsonStr);
                taskInst.setOutputJson(outputJson);
            }
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Error parsing JSON data", e);
            throw new SQLException("Error parsing JSON data", e);
        }
        
        return taskInst;
    }
    
    /**
     * Get tasks with a specific status for a workflow
     * 
     * @param workflowInstanceId The workflow instance ID
     * @param status The task status
     * @return List of task instances with the specified status
     * @throws SQLException If a database error occurs
     */
    public List<TaskInstance> getTasksByStatus(UUID workflowInstanceId, TaskStatus status) throws SQLException {
        final String sql = "SELECT id, workflow_instance_id, task_def_id, task_group_instance_id, assignee, status, " +
                           "input_json, output_json, start_time, end_time, due_date, failure_reason " +
                           "FROM task_instances " +
                           "WHERE workflow_instance_id = ? AND status = ?";
        
        List<TaskInstance> taskInsts = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowInstanceId);
            stmt.setString(2, status.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    taskInsts.add(mapResultSetToTaskInstance(rs));
                }
            }
            
            return taskInsts;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting tasks by status", e);
            throw e;
        }
    }
    
    /**
     * Get expired task instances
     * 
     * @return List of expired task instances
     * @throws SQLException If a database error occurs
     */
    public List<TaskInstance> getExpiredTasks() throws SQLException {
        final String sql = "SELECT id, workflow_instance_id, task_def_id, task_group_instance_id, assignee, status, " +
                           "input_json, output_json, start_time, end_time, due_date, failure_reason " +
                           "FROM task_instances " +
                           "WHERE status IN ('NOT_STARTED', 'IN_PROGRESS') AND due_date < ?";
        
        List<TaskInstance> taskInsts = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    taskInsts.add(mapResultSetToTaskInstance(rs));
                }
            }
            
            return taskInsts;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting expired tasks", e);
            throw e;
        }
    }
}