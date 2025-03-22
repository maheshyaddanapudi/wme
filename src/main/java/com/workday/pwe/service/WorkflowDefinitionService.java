package com.workday.pwe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workday.pwe.dao.TaskDefinitionDAO;
import com.workday.pwe.dao.TaskGroupDefinitionDAO;
import com.workday.pwe.dao.WorkflowDefinitionDAO;
import com.workday.pwe.execution.WorkflowJsonParser;
import com.workday.pwe.model.TaskDefinition;
import com.workday.pwe.model.TaskGroupDefinition;
import com.workday.pwe.model.WorkflowDefinition;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for workflow definition operations.
 */
public class WorkflowDefinitionService {

    private static final Logger LOGGER = Logger.getLogger(WorkflowDefinitionService.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Create a new workflow definition
     * 
     * @param connection Database connection
     * @param name The workflow name
     * @param jsonDefinition The JSON workflow definition
     * @return The created workflow definition
     * @throws Exception If an error occurs
     */
    public WorkflowDefinition createWorkflowDefinition(Connection connection, String name, String jsonDefinition) throws Exception {
        try {
            // Parse and validate the JSON definition
            JsonNode definitionJson = OBJECT_MAPPER.readTree(jsonDefinition);
            
            // Check if a workflow with this name already exists
            WorkflowDefinitionDAO workflowDefDAO = new WorkflowDefinitionDAO(connection);
            WorkflowDefinition existingDef = workflowDefDAO.getLatestWorkflowDefinitionByName(name);
            
            if (existingDef != null) {
                // Create a new version of the existing workflow
                WorkflowDefinition newVersionDef = existingDef.createNewVersion(definitionJson);
                return WorkflowJsonParser.parseWorkflowDefinition(connection, name, jsonDefinition);
            } else {
                // Create a new workflow definition
                return WorkflowJsonParser.parseWorkflowDefinition(connection, name, jsonDefinition);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating workflow definition", e);
            throw e;
        }
    }
    
    /**
     * Get a workflow definition by ID
     * 
     * @param connection Database connection
     * @param id The workflow definition ID
     * @return The workflow definition, or null if not found
     * @throws SQLException If a database error occurs
     */
    public WorkflowDefinition getWorkflowDefinition(Connection connection, UUID id) throws SQLException {
        try {
            WorkflowDefinitionDAO workflowDefDAO = new WorkflowDefinitionDAO(connection);
            return workflowDefDAO.getWorkflowDefinition(id);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting workflow definition", e);
            throw e;
        }
    }
    
    /**
     * Get the latest version of a workflow definition by name
     * 
     * @param connection Database connection
     * @param name The workflow name
     * @return The latest version of the workflow definition, or null if not found
     * @throws SQLException If a database error occurs
     */
    public WorkflowDefinition getLatestWorkflowDefinitionByName(Connection connection, String name) throws SQLException {
        try {
            WorkflowDefinitionDAO workflowDefDAO = new WorkflowDefinitionDAO(connection);
            return workflowDefDAO.getLatestWorkflowDefinitionByName(name);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting latest workflow definition by name", e);
            throw e;
        }
    }
    
    /**
     * Get all workflow definitions
     * 
     * @param connection Database connection
     * @return List of all workflow definitions
     * @throws SQLException If a database error occurs
     */
    public List<WorkflowDefinition> getAllWorkflowDefinitions(Connection connection) throws SQLException {
        try {
            WorkflowDefinitionDAO workflowDefDAO = new WorkflowDefinitionDAO(connection);
            return workflowDefDAO.getAllWorkflowDefinitions();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting all workflow definitions", e);
            throw e;
        }
    }
    
    /**
     * Update a workflow definition - creates a new version
     * 
     * @param connection Database connection
     * @param id The workflow definition ID
     * @param jsonDefinition The new JSON workflow definition
     * @return The updated workflow definition
     * @throws Exception If an error occurs
     */
    public WorkflowDefinition updateWorkflowDefinition(Connection connection, UUID id, String jsonDefinition) throws Exception {
        try {
            // Parse and validate the JSON definition
            JsonNode definitionJson = OBJECT_MAPPER.readTree(jsonDefinition);
            
            WorkflowDefinitionDAO workflowDefDAO = new WorkflowDefinitionDAO(connection);
            WorkflowDefinition existingDef = workflowDefDAO.getWorkflowDefinition(id);
            
            if (existingDef == null) {
                throw new IllegalArgumentException("Workflow definition not found: " + id);
            }
            
            // Create a new version of the workflow
            WorkflowDefinition newVersionDef = existingDef.createNewVersion(definitionJson);
            
            // Parse and create the new workflow version with all its components
            return WorkflowJsonParser.parseWorkflowDefinition(connection, existingDef.getName(), jsonDefinition);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating workflow definition", e);
            throw e;
        }
    }
    
    /**
     * Delete a workflow definition
     * 
     * @param connection Database connection
     * @param id The workflow definition ID
     * @throws SQLException If a database error occurs
     */
    public void deleteWorkflowDefinition(Connection connection, UUID id) throws SQLException {
        try {
            // First, delete all related task definitions and task group definitions
            TaskDefinitionDAO taskDefDAO = new TaskDefinitionDAO(connection);
            List<TaskDefinition> taskDefs = taskDefDAO.getTaskDefinitionsByWorkflowId(id);
            for (TaskDefinition taskDef : taskDefs) {
                taskDefDAO.deleteTaskDefinition(taskDef.getId());
            }
            
            TaskGroupDefinitionDAO groupDefDAO = new TaskGroupDefinitionDAO(connection);
            List<TaskGroupDefinition> groupDefs = groupDefDAO.getTaskGroupsByWorkflowId(id);
            for (TaskGroupDefinition groupDef : groupDefs) {
                groupDefDAO.deleteTaskGroupDefinition(groupDef.getId());
            }
            
            // Finally, delete the workflow definition
            WorkflowDefinitionDAO workflowDefDAO = new WorkflowDefinitionDAO(connection);
            workflowDefDAO.deleteWorkflowDefinition(id);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting workflow definition", e);
            throw e;
        }
    }
    
    /**
     * Get all versions of a workflow definition by name
     * 
     * @param connection Database connection
     * @param name The workflow name
     * @return List of all versions of the workflow definition
     * @throws SQLException If a database error occurs
     */
    public List<WorkflowDefinition> getAllVersionsByName(Connection connection, String name) throws SQLException {
        try {
            WorkflowDefinitionDAO workflowDefDAO = new WorkflowDefinitionDAO(connection);
            return workflowDefDAO.getAllVersionsByName(name);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting all versions of workflow definition", e);
            throw e;
        }
    }
    
    /**
     * Generate the JSON representation of a workflow definition
     * 
     * @param connection Database connection
     * @param id The workflow definition ID
     * @return The JSON representation of the workflow definition
     * @throws Exception If an error occurs
     */
    public JsonNode getWorkflowDefinitionJson(Connection connection, UUID id) throws Exception {
        try {
            return WorkflowJsonParser.generateWorkflowJson(connection, id);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating workflow JSON", e);
            throw e;
        }
    }
    
    /**
     * Validate a workflow definition JSON
     * 
     * @param jsonDefinition The JSON workflow definition to validate
     * @return true if the definition is valid, false otherwise
     */
    public boolean validateWorkflowDefinition(String jsonDefinition) {
        try {
            JsonNode definitionJson = OBJECT_MAPPER.readTree(jsonDefinition);
            
            // Perform validation checks - this is a simplified validation
            if (!definitionJson.has("id") || 
                !definitionJson.has("type") || 
                !"group".equals(definitionJson.get("type").asText())) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error validating workflow definition", e);
            return false;
        }
    }
}
