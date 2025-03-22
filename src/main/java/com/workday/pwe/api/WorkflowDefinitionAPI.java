package com.workday.pwe.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.workday.pwe.model.WorkflowDefinition;

import java.util.List;
import java.util.UUID;

/**
 * API interface for workflow definition operations.
 */
public interface WorkflowDefinitionAPI {

    /**
     * Creates a new workflow definition
     * 
     * @param name Workflow name
     * @param jsonDefinition JSON workflow definition
     * @return The created workflow definition
     */
    WorkflowDefinition createDefinition(String name, String jsonDefinition);
    
    /**
     * Gets a workflow definition by ID
     * 
     * @param id Workflow definition ID
     * @return The workflow definition
     */
    WorkflowDefinition getDefinition(UUID id);
    
    /**
     * Gets latest version of a workflow definition by name
     * 
     * @param name Workflow name
     * @return The latest workflow definition
     */
    WorkflowDefinition getDefinitionByName(String name);
    
    /**
     * Gets all workflow definitions
     * 
     * @return List of all workflow definitions
     */
    List<WorkflowDefinition> getAllDefinitions();
    
    /**
     * Updates a workflow definition (creates a new version)
     * 
     * @param id Workflow definition ID
     * @param jsonDefinition New JSON workflow definition
     * @return The updated workflow definition
     */
    WorkflowDefinition updateDefinition(UUID id, String jsonDefinition);
    
    /**
     * Deletes a workflow definition
     * 
     * @param id Workflow definition ID
     */
    void deleteDefinition(UUID id);
    
    /**
     * Gets all versions of a workflow definition
     * 
     * @param name Workflow name
     * @return List of all versions
     */
    List<WorkflowDefinition> getAllVersions(String name);
    
    /**
     * Validates a workflow definition JSON
     * 
     * @param jsonDefinition JSON workflow definition
     * @return True if valid, false otherwise
     */
    boolean validateDefinition(String jsonDefinition);
}