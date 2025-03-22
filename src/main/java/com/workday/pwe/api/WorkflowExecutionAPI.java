package com.workday.pwe.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.workday.pwe.model.WorkflowInstance;

import java.util.List;
import java.util.UUID;

/**
 * API interface for workflow execution operations.
 */
public interface WorkflowExecutionAPI {

    /**
     * Starts a new workflow instance
     * 
     * @param workflowDefId Workflow definition ID
     * @param inputJson Input data
     * @return The created workflow instance
     */
    WorkflowInstance startWorkflow(UUID workflowDefId, JsonNode inputJson);
    
    /**
     * Gets a workflow instance by ID
     * 
     * @param id Workflow instance ID
     * @return The workflow instance
     */
    WorkflowInstance getWorkflow(UUID id);
    
    /**
     * Gets workflow instances for a workflow definition
     * 
     * @param workflowDefId Workflow definition ID
     * @return List of workflow instances
     */
    List<WorkflowInstance> getWorkflowsByDefinition(UUID workflowDefId);
    
    /**
     * Pauses a running workflow
     * 
     * @param id Workflow instance ID
     * @return True if paused successfully, false otherwise
     */
    boolean pauseWorkflow(UUID id);
    
    /**
     * Resumes a paused workflow
     * 
     * @param id Workflow instance ID
     * @return True if resumed successfully, false otherwise
     */
    boolean resumeWorkflow(UUID id);
    
    /**
     * Terminates a workflow
     * 
     * @param id Workflow instance ID
     * @param reason Reason for termination
     * @return True if terminated successfully, false otherwise
     */
    boolean terminateWorkflow(UUID id, String reason);
    
    /**
     * Gets workflow history
     * 
     * @param id Workflow instance ID
     * @return History records as JSON
     */
    JsonNode getWorkflowHistory(UUID id);
}