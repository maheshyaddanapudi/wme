package com.workday.pwe.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.workday.pwe.api.WorkflowDefinitionAPI;
import com.workday.pwe.model.WorkflowDefinition;
import com.workday.pwe.service.WorkflowDefinitionService;
import com.workday.pwe.service.WorkflowVersionService;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the WorkflowDefinitionAPI interface.
 */
public class WorkflowDefinitionAPIImpl implements WorkflowDefinitionAPI {

    private static final Logger LOGGER = Logger.getLogger(WorkflowDefinitionAPIImpl.class.getName());
    
    private final Connection connection;
    private final WorkflowDefinitionService workflowDefService;
    private final WorkflowVersionService workflowVersionService;
    
    /**
     * Constructor with dependencies
     * 
     * @param connection Database connection
     */
    public WorkflowDefinitionAPIImpl(Connection connection) {
        this.connection = connection;
        this.workflowDefService = new WorkflowDefinitionService();
        this.workflowVersionService = new WorkflowVersionService();
    }

    @Override
    public WorkflowDefinition createDefinition(String name, String jsonDefinition) {
        try {
            return workflowDefService.createWorkflowDefinition(connection, name, jsonDefinition);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating workflow definition", e);
            throw new RuntimeException("Error creating workflow definition", e);
        }
    }

    @Override
    public WorkflowDefinition getDefinition(UUID id) {
        try {
            return workflowDefService.getWorkflowDefinition(connection, id);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting workflow definition", e);
            throw new RuntimeException("Error getting workflow definition", e);
        }
    }

    @Override
    public WorkflowDefinition getDefinitionByName(String name) {
        try {
            return workflowDefService.getLatestWorkflowDefinitionByName(connection, name);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting workflow definition by name", e);
            throw new RuntimeException("Error getting workflow definition by name", e);
        }
    }

    @Override
    public List<WorkflowDefinition> getAllDefinitions() {
        try {
            return workflowDefService.getAllWorkflowDefinitions(connection);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting all workflow definitions", e);
            throw new RuntimeException("Error getting all workflow definitions", e);
        }
    }

    @Override
    public WorkflowDefinition updateDefinition(UUID id, String jsonDefinition) {
        try {
            return workflowDefService.updateWorkflowDefinition(connection, id, jsonDefinition);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating workflow definition", e);
            throw new RuntimeException("Error updating workflow definition", e);
        }
    }

    @Override
    public void deleteDefinition(UUID id) {
        try {
            workflowDefService.deleteWorkflowDefinition(connection, id);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting workflow definition", e);
            throw new RuntimeException("Error deleting workflow definition", e);
        }
    }

    @Override
    public List<WorkflowDefinition> getAllVersions(String name) {
        try {
            return workflowVersionService.getAllVersions(connection, name);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting all workflow versions", e);
            throw new RuntimeException("Error getting all workflow versions", e);
        }
    }

    @Override
    public boolean validateDefinition(String jsonDefinition) {
        try {
            return workflowDefService.validateWorkflowDefinition(jsonDefinition);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error validating workflow definition", e);
            return false;
        }
    }
}