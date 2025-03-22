package com.workday.pwe.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.workday.pwe.model.TaskInstance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * API interface for task management operations.
 */
public interface TaskManagementAPI {

    /**
     * Gets a task instance by ID
     * 
     * @param taskId Task instance ID
     * @return The task instance
     */
    TaskInstance getTaskDetails(UUID taskId);
    
    /**
     * Gets tasks for a workflow
     * 
     * @param workflowInstanceId Workflow instance ID
     * @return List of task instances
     */
    List<TaskInstance> getTasksForWorkflow(UUID workflowInstanceId);
    
    /**
     * Completes a task
     * 
     * @param taskId Task instance ID
     * @param outputJson Output data
     * @return True if successful, false otherwise
     */
    boolean completeTask(UUID taskId, JsonNode outputJson);
    
    /**
     * Submits a task
     * 
     * @param taskId Task instance ID
     * @param outputJson Output data
     * @return True if successful, false otherwise
     */
    boolean submitTask(UUID taskId, JsonNode outputJson);
    
    /**
     * Approves a task
     * 
     * @param taskId Task instance ID
     * @param outputJson Output data
     * @return True if successful, false otherwise
     */
    boolean approveTask(UUID taskId, JsonNode outputJson);
    
    /**
     * Fails a task
     * 
     * @param taskId Task instance ID
     * @param reason Failure reason
     * @return True if successful, false otherwise
     */
    boolean failTask(UUID taskId, String reason);
    
    /**
     * Updates task assignment
     * 
     * @param taskId Task instance ID
     * @param newAssignee New assignee
     * @return The updated task instance
     */
    TaskInstance updateTaskAssignment(UUID taskId, String newAssignee);
    
    /**
     * Updates task due date
     * 
     * @param taskId Task instance ID
     * @param newDueDate New due date
     * @return The updated task instance
     */
    TaskInstance updateTaskDueDate(UUID taskId, LocalDateTime newDueDate);
    
    /**
     * Resubmits a failed task
     * 
     * @param taskId Task instance ID
     * @return True if successful, false otherwise
     */
    boolean resubmitTask(UUID taskId);
}