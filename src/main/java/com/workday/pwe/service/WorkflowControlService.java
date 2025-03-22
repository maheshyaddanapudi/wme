package com.workday.pwe.service;

import com.workday.pwe.dao.WorkflowInstanceDAO;
import com.workday.pwe.enums.WorkflowStatus;
import com.workday.pwe.execution.ExecutionQueuingInterceptor;
import com.workday.pwe.model.WorkflowInstance;

import java.sql.Connection;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for workflow control operations (pause, resume, terminate).
 */
public class WorkflowControlService {

    private static final Logger LOGGER = Logger.getLogger(WorkflowControlService.class.getName());

    /**
     * Pauses a running workflow
     * 
     * @param connection Database connection
     * @param workflowInstanceId Workflow instance ID
     * @return True if workflow was paused, false otherwise
     */
    public boolean pauseWorkflow(Connection connection, UUID workflowInstanceId) {
        try {
            WorkflowInstanceDAO workflowInstDAO = new WorkflowInstanceDAO(connection);
            WorkflowInstance workflow = workflowInstDAO.getWorkflowInstance(workflowInstanceId);
            
            if (workflow == null) {
                LOGGER.warning("Workflow not found: " + workflowInstanceId);
                return false;
            }
            
            if (workflow.getStatus() != WorkflowStatus.RUNNING) {
                LOGGER.warning("Cannot pause workflow with status: " + workflow.getStatus());
                return false;
            }
            
            workflowInstDAO.updateWorkflowStatus(workflowInstanceId, WorkflowStatus.PAUSED);
            
            // Remove from execution queue if present
            ExecutionQueuingInterceptor.removeFromQueue(connection, workflowInstanceId);
            
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error pausing workflow", e);
            throw new RuntimeException("Error pausing workflow", e);
        }
    }
    
    /**
     * Resumes a paused workflow
     * 
     * @param connection Database connection
     * @param workflowInstanceId Workflow instance ID
     * @return True if workflow was resumed, false otherwise
     */
    public boolean resumeWorkflow(Connection connection, UUID workflowInstanceId) {
        try {
            WorkflowInstanceDAO workflowInstDAO = new WorkflowInstanceDAO(connection);
            WorkflowInstance workflow = workflowInstDAO.getWorkflowInstance(workflowInstanceId);
            
            if (workflow == null) {
                LOGGER.warning("Workflow not found: " + workflowInstanceId);
                return false;
            }
            
            if (workflow.getStatus() != WorkflowStatus.PAUSED) {
                LOGGER.warning("Cannot resume workflow with status: " + workflow.getStatus());
                return false;
            }
            
            workflowInstDAO.updateWorkflowStatus(workflowInstanceId, WorkflowStatus.RUNNING);
            
            // Queue for state management
            ExecutionQueuingInterceptor.queueForStateManagement(connection, workflowInstanceId);
            
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error resuming workflow", e);
            throw new RuntimeException("Error resuming workflow", e);
        }
    }
    
    /**
     * Terminates a workflow
     * 
     * @param connection Database connection
     * @param workflowInstanceId Workflow instance ID
     * @param reason Reason for termination
     * @return True if workflow was terminated, false otherwise
     */
    public boolean terminateWorkflow(Connection connection, UUID workflowInstanceId, String reason) {
        try {
            WorkflowInstanceDAO workflowInstDAO = new WorkflowInstanceDAO(connection);
            WorkflowInstance workflow = workflowInstDAO.getWorkflowInstance(workflowInstanceId);
            
            if (workflow == null) {
                LOGGER.warning("Workflow not found: " + workflowInstanceId);
                return false;
            }
            
            // Only running or paused workflows can be terminated
            if (workflow.getStatus() != WorkflowStatus.RUNNING && 
                workflow.getStatus() != WorkflowStatus.PAUSED) {
                LOGGER.warning("Cannot terminate workflow with status: " + workflow.getStatus());
                return false;
            }
            
            workflowInstDAO.updateWorkflowStatus(workflowInstanceId, WorkflowStatus.TERMINATED);
            
            // Remove from execution queue if present
            ExecutionQueuingInterceptor.removeFromQueue(connection, workflowInstanceId);
            
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error terminating workflow", e);
            throw new RuntimeException("Error terminating workflow", e);
        }
    }
}