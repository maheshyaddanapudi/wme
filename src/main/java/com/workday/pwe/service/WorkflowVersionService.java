package com.workday.pwe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.workday.pwe.dao.WorkflowDefinitionDAO;
import com.workday.pwe.model.WorkflowDefinition;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for workflow versioning operations.
 */
public class WorkflowVersionService {

    private static final Logger LOGGER = Logger.getLogger(WorkflowVersionService.class.getName());

    /**
     * Creates a new version of an existing workflow definition
     * 
     * @param connection Database connection
     * @param workflowDefId Original workflow definition ID
     * @param definitionJson New definition JSON
     * @return The new workflow definition version
     */
    public WorkflowDefinition createNewVersion(Connection connection, UUID workflowDefId, JsonNode definitionJson) {
        try {
            WorkflowDefinitionDAO workflowDefDAO = new WorkflowDefinitionDAO(connection);
            WorkflowDefinition currentDefinition = workflowDefDAO.getWorkflowDefinition(workflowDefId);
            
            if (currentDefinition == null) {
                throw new IllegalArgumentException("Workflow definition not found: " + workflowDefId);
            }
            
            WorkflowDefinition newVersion = currentDefinition.createNewVersion(definitionJson);
            UUID newVersionId = workflowDefDAO.createWorkflowDefinition(newVersion);
            newVersion.setId(newVersionId);
            
            return newVersion;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating new workflow version", e);
            throw new RuntimeException("Error creating new workflow version", e);
        }
    }
    
    /**
     * Gets all versions of a workflow definition
     * 
     * @param connection Database connection
     * @param name Workflow definition name
     * @return List of all versions
     */
    public List<WorkflowDefinition> getAllVersions(Connection connection, String name) {
        try {
            WorkflowDefinitionDAO workflowDefDAO = new WorkflowDefinitionDAO(connection);
            return workflowDefDAO.getAllVersionsByName(name);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving workflow versions", e);
            throw new RuntimeException("Error retrieving workflow versions", e);
        }
    }
    
    /**
     * Gets a specific version of a workflow
     * 
     * @param connection Database connection
     * @param name Workflow definition name
     * @param version Version number
     * @return The workflow definition, or null if not found
     */
    public WorkflowDefinition getSpecificVersion(Connection connection, String name, int version) {
        try {
            List<WorkflowDefinition> allVersions = getAllVersions(connection, name);
            return allVersions.stream()
                    .filter(def -> def.getVersion() == version)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving specific workflow version", e);
            throw new RuntimeException("Error retrieving specific workflow version", e);
        }
    }
}