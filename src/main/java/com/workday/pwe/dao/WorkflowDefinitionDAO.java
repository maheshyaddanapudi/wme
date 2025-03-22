package com.workday.pwe.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workday.pwe.model.WorkflowDefinition;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for workflow_definitions table operations.
 */
public class WorkflowDefinitionDAO {

    private static final Logger LOGGER = Logger.getLogger(WorkflowDefinitionDAO.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final Connection connection;
    
    /**
     * Constructor with database connection
     * 
     * @param connection The database connection
     */
    public WorkflowDefinitionDAO(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.connection = connection;
    }
    
    /**
     * Create a new workflow definition
     * 
     * @param workflowDef The workflow definition to create
     * @return The ID of the created workflow definition
     * @throws SQLException If a database error occurs
     */
    public UUID createWorkflowDefinition(WorkflowDefinition workflowDef) throws SQLException {
        final String sql = "INSERT INTO workflow_definitions " +
                           "(id, name, version, definition_json, description, created_at, updated_at) " +
                           "VALUES (?, ?, ?, ?::jsonb, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            UUID id = workflowDef.getId() != null ? workflowDef.getId() : UUID.randomUUID();
            
            stmt.setObject(1, id);
            stmt.setString(2, workflowDef.getName());
            stmt.setInt(3, workflowDef.getVersion());
            stmt.setString(4, workflowDef.getDefinitionJson().toString());
            stmt.setString(5, workflowDef.getDescription());
            stmt.setTimestamp(6, Timestamp.valueOf(workflowDef.getCreatedAt() != null ? 
                                                 workflowDef.getCreatedAt() : LocalDateTime.now()));
            stmt.setTimestamp(7, Timestamp.valueOf(workflowDef.getUpdatedAt() != null ? 
                                                 workflowDef.getUpdatedAt() : LocalDateTime.now()));
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating workflow definition failed, no rows affected.");
            }
            
            return id;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating workflow definition", e);
            throw e;
        }
    }
    
    /**
     * Get a workflow definition by ID
     * 
     * @param id The workflow definition ID
     * @return The workflow definition, or null if not found
     * @throws SQLException If a database error occurs
     */
    public WorkflowDefinition getWorkflowDefinition(UUID id) throws SQLException {
        final String sql = "SELECT id, name, version, definition_json, description, created_at, updated_at " +
                           "FROM workflow_definitions WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToWorkflowDefinition(rs);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting workflow definition", e);
            throw e;
        }
    }
    
    /**
     * Get the latest version of a workflow definition by name
     * 
     * @param name The workflow definition name
     * @return The latest version of the workflow definition, or null if not found
     * @throws SQLException If a database error occurs
     */
    public WorkflowDefinition getLatestWorkflowDefinitionByName(String name) throws SQLException {
        final String sql = "SELECT id, name, version, definition_json, description, created_at, updated_at " +
                           "FROM workflow_definitions WHERE name = ? " +
                           "ORDER BY version DESC LIMIT 1";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToWorkflowDefinition(rs);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting latest workflow definition", e);
            throw e;
        }
    }
    
    /**
     * Get all workflow definitions
     * 
     * @return List of all workflow definitions
     * @throws SQLException If a database error occurs
     */
    public List<WorkflowDefinition> getAllWorkflowDefinitions() throws SQLException {
        final String sql = "SELECT id, name, version, definition_json, description, created_at, updated_at " +
                           "FROM workflow_definitions";
        
        List<WorkflowDefinition> workflows = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                workflows.add(mapResultSetToWorkflowDefinition(rs));
            }
            
            return workflows;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting all workflow definitions", e);
            throw e;
        }
    }
    
    /**
     * Update a workflow definition
     * 
     * @param workflowDef The workflow definition to update
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int updateWorkflowDefinition(WorkflowDefinition workflowDef) throws SQLException {
        final String sql = "UPDATE workflow_definitions SET " +
                           "name = ?, version = ?, definition_json = ?::jsonb, description = ?, updated_at = ? " +
                           "WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, workflowDef.getName());
            stmt.setInt(2, workflowDef.getVersion());
            stmt.setString(3, workflowDef.getDefinitionJson().toString());
            stmt.setString(4, workflowDef.getDescription());
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setObject(6, workflowDef.getId());
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating workflow definition", e);
            throw e;
        }
    }
    
    /**
     * Delete a workflow definition
     * 
     * @param id The workflow definition ID
     * @return The number of rows affected
     * @throws SQLException If a database error occurs
     */
    public int deleteWorkflowDefinition(UUID id) throws SQLException {
        final String sql = "DELETE FROM workflow_definitions WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting workflow definition", e);
            throw e;
        }
    }
    
    /**
     * Map a result set row to a WorkflowDefinition object
     * 
     * @param rs The result set
     * @return The mapped WorkflowDefinition
     * @throws SQLException If a database error occurs
     */
    private WorkflowDefinition mapResultSetToWorkflowDefinition(ResultSet rs) throws SQLException {
        WorkflowDefinition workflowDef = new WorkflowDefinition();
        
        workflowDef.setId(UUID.fromString(rs.getString("id")));
        workflowDef.setName(rs.getString("name"));
        workflowDef.setVersion(rs.getInt("version"));
        workflowDef.setDescription(rs.getString("description"));
        workflowDef.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        workflowDef.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        
        // Parse the JSON definition
        try {
            String jsonStr = rs.getString("definition_json");
            JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonStr);
            workflowDef.setDefinitionJson(jsonNode);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Error parsing definition JSON", e);
            throw new SQLException("Error parsing definition JSON", e);
        }
        
        return workflowDef;
    }
    
    /**
     * Check if a workflow definition exists
     * 
     * @param id The workflow definition ID
     * @return true if the workflow definition exists, false otherwise
     * @throws SQLException If a database error occurs
     */
    public boolean workflowDefinitionExists(UUID id) throws SQLException {
        final String sql = "SELECT 1 FROM workflow_definitions WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if workflow definition exists", e);
            throw e;
        }
    }
    
    /**
     * Get all versions of a workflow definition by name
     * 
     * @param name The workflow definition name
     * @return List of all versions of the workflow definition
     * @throws SQLException If a database error occurs
     */
    public List<WorkflowDefinition> getAllVersionsByName(String name) throws SQLException {
        final String sql = "SELECT id, name, version, definition_json, description, created_at, updated_at " +
                           "FROM workflow_definitions WHERE name = ? " +
                           "ORDER BY version ASC";
        
        List<WorkflowDefinition> workflows = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    workflows.add(mapResultSetToWorkflowDefinition(rs));
                }
            }
            
            return workflows;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting all versions of workflow definition", e);
            throw e;
        }
    }
}