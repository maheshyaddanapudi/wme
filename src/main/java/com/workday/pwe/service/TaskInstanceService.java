package com.workday.pwe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.workday.pwe.dao.TaskDefinitionDAO;
import com.workday.pwe.dao.TaskInstanceDAO;
import com.workday.pwe.dao.WorkflowInstanceDAO;
import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.enums.WorkflowStatus;
import com.workday.pwe.model.TaskDefinition;
import com.workday.pwe.model.TaskInstance;
import com.workday.pwe.model.WorkflowInstance;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for task instance operations.
 */
public class TaskInstanceService {

    private static final Logger LOGGER = Logger.getLogger(TaskInstanceService.class.getName());

    /**
     * Creates a new task instance
     * 
     * @param connection Database connection
     * @param workflowInstanceId Workflow instance ID
     * @param taskDefId Task definition ID
     * @param assignee Task assignee
     * @param inputJson Input parameters
     * @return The created task instance
     */
    public TaskInstance createTaskInstance(Connection connection, UUID workflowInstanceId, 
                                          UUID taskDefId, String assignee, JsonNode inputJson) {
        try {
            // Verify workflow exists and is in a valid state
            WorkflowInstanceDAO workflowInstDAO = new WorkflowInstanceDAO(connection);
            WorkflowInstance workflow = workflowInstDAO.getWorkflowInstance(workflowInstanceId);
            
            if (workflow == null) {
                throw new IllegalArgumentException("Workflow instance not found: " + workflowInstanceId);
            }
            
            if (workflow.getStatus() != WorkflowStatus.RUNNING) {
                throw new IllegalStateException("Cannot create task for workflow with status: " + workflow.getStatus());
            }
            
            // Verify task definition exists
            TaskDefinitionDAO taskDefDAO = new TaskDefinitionDAO(connection);
            TaskDefinition taskDef = taskDefDAO.getTaskDefinition(taskDefId);
            
            if (taskDef == null) {
                throw new IllegalArgumentException("Task definition not found: " + taskDefId);
            }
            
            // Create the task instance
            TaskInstance taskInst = new TaskInstance(workflowInstanceId, taskDefId, assignee);
            taskInst.setInputJson(inputJson);
            
            TaskInstanceDAO taskInstDAO = new TaskInstanceDAO(connection);
            UUID taskInstId = taskInstDAO.createTaskInstance(taskInst);
            taskInst.setId(taskInstId);
            
            return taskInst;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating task instance", e);
            throw new RuntimeException("Error creating task instance", e);
        }
    }
    
    /**
     * Gets a task instance by ID
     * 
     * @param connection Database connection
     * @param taskInstanceId Task instance ID
     * @return The task instance, or null if not found
     */
    public TaskInstance getTaskInstance(Connection connection, UUID taskInstanceId) {
        try {
            TaskInstanceDAO taskInstDAO = new TaskInstanceDAO(connection);
            return taskInstDAO.getTaskInstance(taskInstanceId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting task instance", e);
            throw new RuntimeException("Error getting task instance", e);
        }
    }
    
    /**
     * Gets task instances for a workflow
     * 
     * @param connection Database connection
     * @param workflowInstanceId Workflow instance ID
     * @return List of task instances
     */
    public List<TaskInstance> getTasksForWorkflow(Connection connection, UUID workflowInstanceId) {
        try {
            TaskInstanceDAO taskInstDAO = new TaskInstanceDAO(connection);
            return taskInstDAO.getTaskInstancesByWorkflowId(workflowInstanceId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting tasks for workflow", e);
            throw new RuntimeException("Error getting tasks for workflow", e);
        }
    }
    
    /**
     * Updates task assignment
     * 
     * @param connection Database connection
     * @param taskInstanceId Task instance ID
     * @param newAssignee New assignee
     * @return The updated task instance
     */
    public TaskInstance updateTaskAssignment(Connection connection, UUID taskInstanceId, String newAssignee) {
        try {
            TaskInstanceDAO taskInstDAO = new TaskInstanceDAO(connection);
            TaskInstance taskInst = taskInstDAO.getTaskInstance(taskInstanceId);
            
            if (taskInst == null) {
                throw new IllegalArgumentException("Task instance not found: " + taskInstanceId);
            }
            
            // Only update if task is not yet completed
            if (taskInst.getStatus() == TaskStatus.NOT_STARTED || 
                taskInst.getStatus() == TaskStatus.IN_PROGRESS ||
                taskInst.getStatus() == TaskStatus.BLOCKED) {
                
                taskInst.setAssignee(newAssignee);
                taskInstDAO.updateTaskInstance(taskInst);
            } else {
                LOGGER.warning("Cannot update assignment for task with status: " + taskInst.getStatus());
            }
            
            return taskInst;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating task assignment", e);
            throw new RuntimeException("Error updating task assignment", e);
        }
    }
    
    /**
     * Updates task due date
     * 
     * @param connection Database connection
     * @param taskInstanceId Task instance ID
     * @param newDueDate New due date
     * @return The updated task instance
     */
    public TaskInstance updateTaskDueDate(Connection connection, UUID taskInstanceId, LocalDateTime newDueDate) {
        try {
            TaskInstanceDAO taskInstDAO = new TaskInstanceDAO(connection);
            TaskInstance taskInst = taskInstDAO.getTaskInstance(taskInstanceId);
            
            if (taskInst == null) {
                throw new IllegalArgumentException("Task instance not found: " + taskInstanceId);
            }
            
            // Only update if task is not yet completed
            if (taskInst.getStatus() == TaskStatus.NOT_STARTED || 
                taskInst.getStatus() == TaskStatus.IN_PROGRESS ||
                taskInst.getStatus() == TaskStatus.BLOCKED) {
                
                taskInst.setDueDate(newDueDate);
                taskInstDAO.updateTaskInstance(taskInst);
            } else {
                LOGGER.warning("Cannot update due date for task with status: " + taskInst.getStatus());
            }
            
            return taskInst;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating task due date", e);
            throw new RuntimeException("Error updating task due date", e);
        }
    }
}