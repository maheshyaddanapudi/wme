package com.workday.pwe.execution;

import com.workday.pwe.dao.WorkflowExecutionQueueDAO;
import com.workday.pwe.enums.QueueStatus;
import com.workday.pwe.model.WorkflowExecutionQueue;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Intercepts workflow completion events and queues them for state management.
 */
public class ExecutionQueuingInterceptor {

    private static final Logger LOGGER = Logger.getLogger(ExecutionQueuingInterceptor.class.getName());
    
    /**
     * Private constructor to prevent instantiation
     */
    private ExecutionQueuingInterceptor() {
        // Do not instantiate
    }
    
    /**
     * Queue a workflow for state management processing
     * 
     * @param connection Database connection
     * @param workflowInstanceId The workflow instance ID to queue
     */
    public static void queueForStateManagement(Connection connection, UUID workflowInstanceId) {
        queueForStateManagement(connection, Collections.singletonList(workflowInstanceId.toString()));
    }
    
    /**
     * Queue multiple workflows for state management processing
     * 
     * @param connection Database connection
     * @param workflowInstanceIds List of workflow instance IDs to queue
     */
    public static void queueForStateManagement(Connection connection, List<String> workflowInstanceIds) {
        try {
            WorkflowExecutionQueueDAO queueDAO = new WorkflowExecutionQueueDAO(connection);
            
            for (String workflowId : workflowInstanceIds) {
                // Check if the workflow is already in the queue
                if (queueDAO.isWorkflowInQueue(UUID.fromString(workflowId))) {
                    // Update its status to PENDING if not already
                    queueDAO.updateQueueStatus(UUID.fromString(workflowId), QueueStatus.PENDING);
                    LOGGER.info("Updated existing queue entry for workflow: " + workflowId);
                } else {
                    // Add a new entry to the queue
                    WorkflowExecutionQueue queueEntry = new WorkflowExecutionQueue(UUID.fromString(workflowId));
                    queueDAO.addToQueue(queueEntry);
                    LOGGER.info("Added workflow to execution queue: " + workflowId);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error queuing workflow for state management", e);
            throw new RuntimeException("Error queuing workflow for state management", e);
        }
    }
    
    /**
     * Queue a workflow for state management with a specific priority
     * 
     * @param connection Database connection
     * @param workflowInstanceId The workflow instance ID to queue
     * @param priority The priority level (higher numbers = higher priority)
     */
    public static void queueWithPriority(Connection connection, UUID workflowInstanceId, int priority) {
        try {
            WorkflowExecutionQueueDAO queueDAO = new WorkflowExecutionQueueDAO(connection);
            
            // Check if the workflow is already in the queue
            if (queueDAO.isWorkflowInQueue(workflowInstanceId)) {
                // Update its status to PENDING and set priority
                queueDAO.updateQueueStatusAndPriority(workflowInstanceId, QueueStatus.PENDING, priority);
                LOGGER.info("Updated existing queue entry for workflow: " + workflowInstanceId + " with priority: " + priority);
            } else {
                // Add a new entry to the queue with the specified priority
                WorkflowExecutionQueue queueEntry = new WorkflowExecutionQueue(workflowInstanceId, priority);
                queueDAO.addToQueue(queueEntry);
                LOGGER.info("Added workflow to execution queue: " + workflowInstanceId + " with priority: " + priority);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error queuing workflow with priority", e);
            throw new RuntimeException("Error queuing workflow with priority", e);
        }
    }
    
    /**
     * Update a queue entry's status
     * 
     * @param connection Database connection
     * @param workflowInstanceId The workflow instance ID
     * @param status The new status
     */
    public static void updateQueueStatus(Connection connection, UUID workflowInstanceId, QueueStatus status) {
        try {
            WorkflowExecutionQueueDAO queueDAO = new WorkflowExecutionQueueDAO(connection);
            queueDAO.updateQueueStatus(workflowInstanceId, status);
            LOGGER.info("Updated queue status for workflow: " + workflowInstanceId + " to: " + status);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating queue status", e);
            throw new RuntimeException("Error updating queue status", e);
        }
    }
    
    /**
     * Remove a workflow from the queue
     * 
     * @param connection Database connection
     * @param workflowInstanceId The workflow instance ID to remove
     */
    public static void removeFromQueue(Connection connection, UUID workflowInstanceId) {
        try {
            WorkflowExecutionQueueDAO queueDAO = new WorkflowExecutionQueueDAO(connection);
            queueDAO.removeFromQueue(workflowInstanceId);
            LOGGER.info("Removed workflow from execution queue: " + workflowInstanceId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error removing workflow from queue", e);
            throw new RuntimeException("Error removing workflow from queue", e);
        }
    }
}