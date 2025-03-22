package com.workday.pwe.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workday.pwe.enums.CompletionCriteria;
import com.workday.pwe.enums.TaskGroupType;
import com.workday.pwe.model.TaskGroupDefinition;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for task_group_definitions table operations.
 */
public class TaskGroupDefinitionDAO {

    private static final Logger LOGGER = Logger.getLogger(TaskGroupDefinitionDAO.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final Connection connection;
    
    /**
     * Constructor with database connection
     * 
     * @param connection The database connection
     */
    public TaskGroupDefinitionDAO(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.connection = connection;
    }
    
    /**
     * Create a new task group definition
     * 
     * @param groupDef The task group definition to create
     * @return The ID of the created task group definition
     * @throws SQLException If a database error occurs
     */
    public UUID createTaskGroupDefinition(TaskGroupDefinition groupDef) throws SQLException {
        final String sql = "INSERT INTO task_group_definitions " +
                           "(id, workflow_def_id, parent_group_def_id, name, group_type, completion_criteria, group_order, parameters_json) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            UUID id = groupDef.getId() != null ? groupDef.getId() : UUID.randomUUID();
            
            stmt.setObject(1, id);
            stmt.setObject(2, groupDef.getWorkflowDefId());
            stmt.setObject(3, groupDef.getParentGroupDefId()); // Can be null
            stmt.setString(4, groupDef.getName());
            stmt.setString(5, groupDef.getGroupType().name());
            stmt.setString(6, groupDef.getCompletionCriteria().name());
            stmt.setInt(7, groupDef.getGroupOrder());
            stmt.setString(8, groupDef.getParametersJson() != null ? groupDef.getParametersJson().toString() : null);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating task group definition failed, no rows affected.");
            }
            
            return id;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating task group definition", e);
            throw e;
        }
    }
    
    /**
     * Get a task group definition by ID
     * 
     * @param id The task group definition ID
     * @return The task group definition, or null if not found
     * @throws SQLException If a database error occurs
     */
    public TaskGroupDefinition getTaskGroupDefinition(UUID id) throws SQLException {
        final String sql = "SELECT id, workflow_def_id, parent_group_def_id, name, group_type, completion_criteria, group_order, parameters_json " +
                           "FROM task_group_definitions WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTaskGroupDefinition(rs);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting task group definition", e);
            throw e;
        }
    }
    
    /**
     * Get all task group definitions for a workflow
     * 
     * @param workflowDefId The workflow definition ID
     * @return List of task group definitions
     * @throws SQLException If a database error occurs
     */
    public List<TaskGroupDefinition> getTaskGroupsByWorkflowId(UUID workflowDefId) throws SQLException {
        final String sql = "SELECT id, workflow_def_id, parent_group_def_id, name, group_type, completion_criteria, group_order, parameters_json " +
                           "FROM task_group_definitions WHERE workflow_def_id = ? " +
                           "ORDER BY group_order ASC";
        
        List<TaskGroupDefinition> groupDefs = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowDefId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    groupDefs.add(mapResultSetToTaskGroupDefinition(rs));
                }
            }
            
            return groupDefs;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting task group definitions by workflow ID", e);
            throw e;
        }
    }
    
    /**
     * Get child task groups for a parent group
     * 
     * @param parentGroupDefId The parent task group definition ID
     * @return List of child task group definitions
     * @throws SQLException If a database error occurs
     */
    public List<TaskGroupDefinition> getChildGroups(UUID parentGroupDefId) throws SQLException {
        final String sql = "SELECT id, workflow_def_id, parent_group_def_id, name, group_type, completion_criteria, group_order, parameters_json " +
                           "FROM task_group_definitions WHERE parent_group_def_id = ? " +
                           "ORDER BY group_order ASC";
        
        List<TaskGroupDefinition> groupDefs = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, parentGroupDefId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    groupDefs.add(mapResultSetToTaskGroupDefinition(rs));
                }
            }
            
            return groupDefs;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting child task group definitions", e);
            throw e;
        }
    }
    
    /**
     * Get root task groups (not in any parent group) for a workflow
     * 
     * @param workflowDefId The workflow definition ID
     * @return List of root task group definitions
     * @throws SQLException If a database error occurs
     */
    public List<TaskGroupDefinition> getRootTaskGroups(UUID workflowDefId) throws SQLException {
        final String sql = "SELECT id, workflow_def_id, parent_group_def_id, name, group_type, completion_criteria, group_order, parameters_json " +
                           "FROM task_group_definitions WHERE workflow_def_id = ? AND parent_group_def_id IS NULL " +
                           "ORDER BY group_order ASC";
        
        List<TaskGroupDefinition> groupDefs = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, workflowDefId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    groupDefs.add(mapResultSetToTaskGroupDefinition(rs));
                }
            }
            
            return groupDefs;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting root task group definitions", e);
            throw e;
        }
    }
    
    /**
     * Update a task group definition
     * 
     * @param groupDef The task group definition to update
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int updateTaskGroupDefinition(TaskGroupDefinition groupDef) throws SQLException {
        final String sql = "UPDATE task_group_definitions SET " +
                           "workflow_def_id = ?, parent_group_def_id = ?, name = ?, group_type = ?, " +
                           "completion_criteria = ?, group_order = ?, parameters_json = ?::jsonb " +
                           "WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, groupDef.getWorkflowDefId());
            stmt.setObject(2, groupDef.getParentGroupDefId());
            stmt.setString(3, groupDef.getName());
            stmt.setString(4, groupDef.getGroupType().name());
            stmt.setString(5, groupDef.getCompletionCriteria().name());
            stmt.setInt(6, groupDef.getGroupOrder());
            stmt.setString(7, groupDef.getParametersJson() != null ? groupDef.getParametersJson().toString() : null);
            stmt.setObject(8, groupDef.getId());
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating task group definition", e);
            throw e;
        }
    }
    
    /**
     * Delete a task group definition
     * 
     * @param id The task group definition ID
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int deleteTaskGroupDefinition(UUID id) throws SQLException {
        final String sql = "DELETE FROM task_group_definitions WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting task group definition", e);
            throw e;
        }
    }
    
    /**
     * Map a result set row to a TaskGroupDefinition object
     * 
     * @param rs The result set
     * @return The mapped TaskGroupDefinition
     * @throws SQLException If a database error occurs
     */
    private TaskGroupDefinition mapResultSetToTaskGroupDefinition(ResultSet rs) throws SQLException {
        TaskGroupDefinition groupDef = new TaskGroupDefinition();
        
        groupDef.setId(UUID.fromString(rs.getString("id")));
        groupDef.setWorkflowDefId(UUID.fromString(rs.getString("workflow_def_id")));
        
        String parentGroupDefId = rs.getString("parent_group_def_id");
        if (parentGroupDefId != null) {
            groupDef.setParentGroupDefId(UUID.fromString(parentGroupDefId));
        }
        
        groupDef.setName(rs.getString("name"));
        groupDef.setGroupType(TaskGroupType.valueOf(rs.getString("group_type")));
        groupDef.setCompletionCriteria(CompletionCriteria.valueOf(rs.getString("completion_criteria")));
        groupDef.setGroupOrder(rs.getInt("group_order"));
        
        // Parse the JSON parameters
        try {
            String jsonStr = rs.getString("parameters_json");
            if (jsonStr != null) {
                JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonStr);
                groupDef.setParametersJson(jsonNode);
            }
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Error parsing parameters JSON", e);
            throw new SQLException("Error parsing parameters JSON", e);
        }
        
        return groupDef;
    }
    
    /**
     * Check if a task group definition exists
     * 
     * @param id The task group definition ID
     * @return true if the task group definition exists, false otherwise
     * @throws SQLException If a database error occurs
     */
    public boolean taskGroupDefinitionExists(UUID id) throws SQLException {
        final String sql = "SELECT 1 FROM task_group_definitions WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if task group definition exists", e);
            throw e;
        }
    }
    
    /**
     * Check if a task group has a specific completion criteria
     * 
     * @param id The task group definition ID
     * @param criteria The completion criteria to check
     * @return true if the task group has the specified completion criteria, false otherwise
     * @throws SQLException If a database error occurs
     */
    public boolean hasCompletionCriteria(UUID id, String criteria) throws SQLException {
        final String sql = "SELECT 1 FROM task_group_definitions WHERE id = ? AND completion_criteria = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            stmt.setString(2, criteria);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking task group completion criteria", e);
            throw e;
        }
    }
    
    /**
     * Check if a task group is a vertical group
     * 
     * @param id The task group definition ID
     * @return true if the task group is a vertical group, false otherwise
     * @throws SQLException If a database error occurs
     */
    public boolean isVerticalGroup(UUID id) throws SQLException {
        final String sql = "SELECT 1 FROM task_group_definitions WHERE id = ? AND group_type = 'VERTICAL'";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if task group is vertical", e);
            throw e;
        }
    }
    
    /**
     * Check if a task group is a horizontal group
     * 
     * @param id The task group definition ID
     * @return true if the task group is a horizontal group, false otherwise
     * @throws SQLException If a database error occurs
     */
    public boolean isHorizontalGroup(UUID id) throws SQLException {
        final String sql = "SELECT 1 FROM task_group_definitions WHERE id = ? AND group_type = 'HORIZONTAL'";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if task group is horizontal", e);
            throw e;
        }
    }
}