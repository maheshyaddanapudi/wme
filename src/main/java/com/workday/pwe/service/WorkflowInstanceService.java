package com.workday.pwe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.workday.pwe.dao.*;
import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.enums.WorkflowStatus;
import com.workday.pwe.execution.ExecutionQueuingInterceptor;
import com.workday.pwe.execution.WorkflowStateManager;
import com.workday.pwe.model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for workflow instance operations.
 */
public class WorkflowInstanceService {

    private static final Logger LOGGER = Logger.getLogger(WorkflowInstanceService.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Start a new workflow instance
     * 
     * @param connection Database connection
     * @param workflowDefId The workflow definition ID
     * @param inputJson The input data
     * @return The created workflow instance
     * @throws Exception If an error occurs
     */
    public WorkflowInstance startWorkflow(Connection connection, UUID workflowDefId, JsonNode inputJson) throws Exception {
        try {
            // Check if the workflow definition exists
            WorkflowDefinitionDAO workflowDefDAO = new WorkflowDefinitionDAO(connection);
            WorkflowDefinition workflowDef = workflowDefDAO.getWorkflowDefinition(workflowDefId);
            
            if (workflowDef == null) {
                throw new IllegalArgumentException("Workflow definition not found: " + workflowDefId);
            }
            
            // Create the workflow instance
            WorkflowInstance workflowInst = new WorkflowInstance(workflowDefId, inputJson);
            
            WorkflowInstanceDAO workflowInstDAO = new WorkflowInstanceDAO(connection);
            UUID workflowInstId = workflowInstDAO.createWorkflowInstance(workflowInst);
            workflowInst.setId(workflowInstId);
            
            // Create the task group instances
            createTaskGroupInstances(connection, workflowInst, workflowDef);
            
            // Create the task instances
            createTaskInstances(connection, workflowInst, workflowDef);
            
            // Start the workflow
            workflowInst.start();
            workflowInstDAO.updateWorkflowInstance(workflowInst);
            
            // Add to the execution queue for processing
            ExecutionQueuingInterceptor.queueForStateManagement(connection, workflowInstId);
            
            return workflowInst;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error starting workflow", e);
            throw e;
        }
    }
    
    /**
     * Create task group instances for a workflow
     * 
     * @param connection Database connection
     * @param workflowInst The workflow instance
     * @param workflowDef The workflow definition
     * @throws SQLException If a database error occurs
     */
    private void createTaskGroupInstances(Connection connection, WorkflowInstance workflowInst, WorkflowDefinition workflowDef) throws SQLException {
        TaskGroupDefinitionDAO groupDefDAO = new TaskGroupDefinitionDAO(connection);
        TaskGroupInstanceDAO groupInstDAO = new TaskGroupInstanceDAO(connection);
        
        // Get all task group definitions for this workflow
        List<TaskGroupDefinition> groupDefs = groupDefDAO.getTaskGroupsByWorkflowId(workflowDef.getId());
        
        // Create instances for all groups
        for (TaskGroupDefinition groupDef : groupDefs) {
            TaskGroupInstance groupInst = new TaskGroupInstance(workflowInst.getId(), groupDef.getId(), TaskStatus.NOT_STARTED);
            
            // Set the min completion (for N_OF_M criteria)
            if (groupDef.getCompletionCriteria().toString().equals("N_OF_M")) {
                // Get the value from the definition (default to 1 if not specified)
                int minCompletion = 1;
                if (groupDef.getParametersJson() != null && groupDef.getParametersJson().has("minCompletion")) {
                    minCompletion = groupDef.getParametersJson().get("minCompletion").asInt(1);
                }
                groupInst.setMinCompletion(minCompletion);
            }
            
            // Set the parent group instance if applicable
            if (groupDef.getParentGroupDefId() != null) {
                // Find the parent group instance ID
                TaskGroupInstance parentGroupInst = findParentGroupInstance(connection, workflowInst.getId(), groupDef.getParentGroupDefId());
                if (parentGroupInst != null) {
                    groupInst.setParentGroupInstId(parentGroupInst.getId());
                }
            }
            
            // Copy parameters from definition to instance
            if (groupDef.getParametersJson() != null) {
                groupInst.setParametersJson(groupDef.getParametersJson());
            }
            
            // Create the group instance
            UUID groupInstId = groupInstDAO.createTaskGroupInstance(groupInst);
            groupInst.setId(groupInstId);
        }
    }
    
    /**
     * Find a parent group instance
     * 
     * @param connection Database connection
     * @param workflowInstId The workflow instance ID
     * @param parentGroupDefId The parent group definition ID
     * @return The parent group instance, or null if not found
     * @throws SQLException If a database error occurs
     */
    private TaskGroupInstance findParentGroupInstance(Connection connection, UUID workflowInstId, UUID parentGroupDefId) throws SQLException {
        TaskGroupInstanceDAO groupInstDAO = new TaskGroupInstanceDAO(connection);
        
        // Get all group instances for this workflow
        List<TaskGroupInstance> groupInsts = groupInstDAO.getTaskGroupsByWorkflowId(workflowInstId);
        
        // Find the one with the matching definition ID
        for (TaskGroupInstance groupInst : groupInsts) {
            if (groupInst.getTaskGroupDefId().equals(parentGroupDefId)) {
                return groupInst;
            }
        }
        
        return null;
    }
    
    /**
     * Create task instances for a workflow
     * 
     * @param connection Database connection
     * @param workflowInst The workflow instance
     * @param workflowDef The workflow definition
     * @throws SQLException If a database error occurs
     */
    private void createTaskInstances(Connection connection, WorkflowInstance workflowInst, WorkflowDefinition workflowDef) throws SQLException {
        TaskDefinitionDAO taskDefDAO = new TaskDefinitionDAO(connection);
        TaskInstanceDAO taskInstDAO = new TaskInstanceDAO(connection);
        TaskGroupInstanceDAO groupInstDAO = new TaskGroupInstanceDAO(connection);
        
        // Get all task definitions for this workflow
        List<TaskDefinition> taskDefs = taskDefDAO.getTaskDefinitionsByWorkflowId(workflowDef.getId());
        
        // Create instances for all tasks
        for (TaskDefinition taskDef : taskDefs) {
            // Get assignee from task definition parameters
            String assignee = null;
            if (taskDef.getParametersJson() != null && taskDef.getParametersJson().has("assignee")) {
                assignee = taskDef.getParametersJson().get("assignee").asText();
            }
            
            TaskInstance taskInst = new TaskInstance(workflowInst.getId(), taskDef.getId(), assignee);
            
            // Set the task group instance if applicable
            if (taskDef.getTaskGroupDefId() != null) {
                // Find the group instance ID
                TaskGroupInstance groupInst = findGroupInstance(connection, workflowInst.getId(), taskDef.getTaskGroupDefId());
                if (groupInst != null) {
                    taskInst.setTaskGroupInstanceId(groupInst.getId());
                }
            }
            
            // Set due date if specified in parameters
            if (taskDef.getParametersJson() != null && taskDef.getParametersJson().has("dueDate")) {
                String dueDateStr = taskDef.getParametersJson().get("dueDate").asText();
                if (dueDateStr != null && !dueDateStr.isEmpty()) {
                    try {
                        // Parse the due date using the appropriate format
                        // This is a simplified example - in real implementation, you'd use a more robust approach
                        taskInst.setDueDate(LocalDateTime.parse(dueDateStr));
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error parsing due date: " + dueDateStr, e);
                    }
                }
            }
            
            // Copy input parameters from definition to instance
            if (taskDef.getParametersJson() != null) {
                taskInst.setInputJson(taskDef.getParametersJson());
            }
            
            // Create the task instance
            UUID taskInstId = taskInstDAO.createTaskInstance(taskInst);
            taskInst.setId(taskInstId);
        }
    }
    
    /**
     * Find a group instance
     * 
     * @param connection Database connection
     * @param workflowInstId The workflow instance ID
     * @param groupDefId The group definition ID
     * @return The group instance, or null if not found
     * @throws SQLException If a database error occurs
     */
    private TaskGroupInstance findGroupInstance(Connection connection, UUID workflowInstId, UUID groupDefId) throws SQLException {
        TaskGroupInstanceDAO groupInstDAO = new TaskGroupInstanceDAO(connection);
        
        // Get all group instances for this workflow
        List<TaskGroupInstance> groupInsts = groupInstDAO.getTaskGroupsByWorkflowId(workflowInstId);
        
        // Find the one with the matching definition ID
        for (TaskGroupInstance groupInst : groupInsts) {
            if (groupInst.getTaskGroupDefId().equals(groupDefId)) {
                return groupInst;
            }
        }
        
        return null;
    }
    
    /**
     * Get a workflow instance by ID
     * 
     * @param connection Database connection
     * @param id The workflow instance ID
     * @return The workflow instance, or null if not found
     * @throws SQLException If a database error occurs
     */
    public WorkflowInstance getWorkflowInstance(Connection connection, UUID id) throws SQLException {
        try {
            WorkflowInstanceDAO workflowInstDAO = new WorkflowInstanceDAO(connection);
            return workflowInstDAO.getWorkflowInstance(id);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting workflow instance", e);
            throw e;
        }
    }
    
    /**
     * Get workflow instances for a workflow definition
     * 
     * @param connection Database connection
     * @param workflowDefId The workflow definition ID
     * @return List of workflow instances
     * @throws SQLException If a database error occurs
     */
    public List<WorkflowInstance> getWorkflowInstancesByDefinitionId(Connection connection, UUID workflowDefId) throws SQLException {
        try {
            WorkflowInstanceDAO workflowInstDAO = new WorkflowInstanceDAO(connection);
            return workflowInstDAO.getWorkflowInstancesByDefinitionId(workflowDefId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting workflow instances by definition ID", e);
            throw e;
        }
    }
    
    /**
     * Get workflow instances by status
     * 
     * @param connection Database connection
     * @param status The workflow status
     * @return List of workflow instances
     * @throws SQLException If a database error occurs
     */
    public List<WorkflowInstance> getWorkflowInstancesByStatus(Connection connection, WorkflowStatus status) throws SQLException {
        try {
            WorkflowInstanceDAO workflowInstDAO = new WorkflowInstanceDAO(connection);
            return workflowInstDAO.getWorkflowInstancesByStatus(status);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting workflow instances by status", e);
            throw e;
        }
    }
    
    /**
     * Update a workflow instance's status
     * 
     * @param connection Database connection
     * @param id The workflow instance ID
     * @param status The new status
     * @throws SQLException If a database error occurs
     */
    public void updateWorkflowStatus(Connection connection, UUID id, WorkflowStatus status) throws SQLException {
        try {
            WorkflowInstanceDAO workflowInstDAO = new WorkflowInstanceDAO(connection);
            
            // Create a history record
            WorkflowInstance workflow = workflowInstDAO.getWorkflowInstance(id);
            if (workflow != null) {
                WorkflowHistoryDAO historyDAO = new WorkflowHistoryDAO(connection);
                
                ObjectNode detailsJson = OBJECT_MAPPER.createObjectNode();
                detailsJson.put("oldStatus", workflow.getStatus().name());
                detailsJson.put("newStatus", status.name());
                
                WorkflowHistory history = new WorkflowHistory(id, "WORKFLOW", id, "STATUS_CHANGE", detailsJson);
                historyDAO.createHistoryRecord(history);
            }
            
            // Update the status
            workflowInstDAO.updateWorkflowStatus(id, status);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating workflow status", e);
            throw e;
        }
    }
    
    /**
     * Complete a workflow instance
     * 
     * @param connection Database connection
     * @param id The workflow instance ID
     * @param outputJson The output data
     * @throws SQLException If a database error occurs
     */
    public void completeWorkflow(Connection connection, UUID id, JsonNode outputJson) throws SQLException {
        try {
            WorkflowInstanceDAO workflowInstDAO = new WorkflowInstanceDAO(connection);
            
            // Create a history record
            WorkflowInstance workflow = workflowInstDAO.getWorkflowInstance(id);
            if (workflow != null) {
                WorkflowHistoryDAO historyDAO = new WorkflowHistoryDAO(connection);
                
                ObjectNode detailsJson = OBJECT_MAPPER.createObjectNode();
                detailsJson.put("oldStatus", workflow.getStatus().name());
                detailsJson.put("newStatus", WorkflowStatus.COMPLETED.name());
                
                WorkflowHistory history = new WorkflowHistory(id, "WORKFLOW", id, "STATUS_CHANGE", detailsJson);
                historyDAO.createHistoryRecord(history);
            }
            
            // Update the output and status
            workflowInstDAO.updateOutputAndStatus(id, outputJson, WorkflowStatus.COMPLETED);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error completing workflow", e);
            throw e;
        }
    }
    
    /**
     * Fail a workflow instance
     * 
     * @param connection Database connection
     * @param id The workflow instance ID
     * @param reason The reason for failure
     * @throws SQLException If a database error occurs
     */
    public void failWorkflow(Connection connection, UUID id, String reason) throws SQLException {
        try {
            WorkflowInstanceDAO workflowInstDAO = new WorkflowInstanceDAO(connection);
            
            // Create a history record
            WorkflowInstance workflow = workflowInstDAO.getWorkflowInstance(id);
            if (workflow != null) {
                WorkflowHistoryDAO historyDAO = new WorkflowHistoryDAO(connection);
                
                ObjectNode detailsJson = OBJECT_MAPPER.createObjectNode();
                detailsJson.put("oldStatus", workflow.getStatus().name());
                detailsJson.put("newStatus", WorkflowStatus.FAILED.name());
                detailsJson.put("reason", reason);
                
                WorkflowHistory history = new WorkflowHistory(id, "WORKFLOW", id, "STATUS_CHANGE", detailsJson);
                historyDAO.createHistoryRecord(history);
                
                // Update the output with the failure reason
                ObjectNode outputJson = OBJECT_MAPPER.createObjectNode();
                outputJson.put("error", true);
                outputJson.put("reason", reason);
                
                // Update the output and status
                workflowInstDAO.updateOutputAndStatus(id, outputJson, WorkflowStatus.FAILED);
            } else {
                // Just update the status
                workflowInstDAO.updateWorkflowStatus(id, WorkflowStatus.FAILED);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error failing workflow", e);
            throw e;
        }
    }
    
    /**
     * Delete a workflow instance
     * 
     * @param connection Database connection
     * @param id The workflow instance ID
     * @throws SQLException If a database error occurs
     */
    public void deleteWorkflowInstance(Connection connection, UUID id) throws SQLException {
        try {
            // First, remove from execution queue if present
            WorkflowExecutionQueueDAO queueDAO = new WorkflowExecutionQueueDAO(connection);
            queueDAO.removeFromQueue(id);
            
            // Delete all task instances
            TaskInstanceDAO taskInstDAO = new TaskInstanceDAO(connection);
            List<TaskInstance> taskInsts = taskInstDAO.getTaskInstancesByWorkflowId(id);
            for (TaskInstance taskInst : taskInsts) {
                taskInstDAO.deleteTaskInstance(taskInst.getId());
            }
            
            // Delete all task group instances
            TaskGroupInstanceDAO groupInstDAO = new TaskGroupInstanceDAO(connection);
            List<TaskGroupInstance> groupInsts = groupInstDAO.getTaskGroupsByWorkflowId(id);
            for (TaskGroupInstance groupInst : groupInsts) {
                groupInstDAO.deleteTaskGroupInstance(groupInst.getId());
            }
            
            // Delete workflow history
            WorkflowHistoryDAO historyDAO = new WorkflowHistoryDAO(connection);
            historyDAO.deleteHistoryByWorkflowId(id);
            
            // Finally, delete the workflow instance
            WorkflowInstanceDAO workflowInstDAO = new WorkflowInstanceDAO(connection);
            workflowInstDAO.deleteWorkflowInstance(id);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting workflow instance", e);
            throw e;
        }
    }
    
    /**
     * Archive old workflow instances
     * 
     * @param connection Database connection
     * @param olderThan The date before which to archive workflows
     * @return The number of workflows archived
     * @throws SQLException If a database error occurs
     */
    public int archiveOldWorkflows(Connection connection, LocalDateTime olderThan) throws SQLException {
        try {
            WorkflowInstanceDAO workflowInstDAO = new WorkflowInstanceDAO(connection);
            return workflowInstDAO.archiveOldWorkflows(olderThan);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error archiving old workflows", e);
            throw e;
        }
    }
}
