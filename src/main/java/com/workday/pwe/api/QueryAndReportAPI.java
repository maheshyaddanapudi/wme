package com.workday.pwe.api;

import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.enums.WorkflowStatus;
import com.workday.pwe.model.TaskInstance;
import com.workday.pwe.model.WorkflowHistory;
import com.workday.pwe.model.WorkflowInstance;

import java.util.List;
import java.util.UUID;

/**
 * API interface for query and reporting operations.
 */
public interface QueryAndReportAPI {

    /**
     * Queries workflows by status
     * 
     * @param status Workflow status
     * @return List of workflow instances
     */
    List<WorkflowInstance> queryWorkflows(WorkflowStatus status);
    
    /**
     * Queries tasks by status
     * 
     * @param status Task status
     * @return List of task instances
     */
    List<TaskInstance> queryTasks(TaskStatus status);
    
    /**
     * Queries tasks by assignee
     * 
     * @param assignee Task assignee
     * @return List of task instances
     */
    List<TaskInstance> queryTasksByAssignee(String assignee);
    
    /**
     * Queries overdue tasks
     * 
     * @return List of overdue task instances
     */
    List<TaskInstance> queryOverdueTasks();
    
    /**
     * Gets audit history for a workflow
     * 
     * @param workflowInstanceId Workflow instance ID
     * @return List of history records
     */
    List<WorkflowHistory> getAuditHistory(UUID workflowInstanceId);
    
    /**
     * Gets audit history for an entity
     * 
     * @param entityType Entity type
     * @param entityId Entity ID
     * @return List of history records
     */
    List<WorkflowHistory> getEntityHistory(String entityType, UUID entityId);
}