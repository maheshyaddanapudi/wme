package com.workday.pwe.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.workday.pwe.api.TaskManagementAPI;
import com.workday.pwe.dao.TaskInstanceDAO;
import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.model.TaskInstance;
import com.workday.pwe.service.TaskCompletionService;
import com.workday.pwe.service.TaskInstanceService;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the TaskManagementAPI interface.
 */
public class TaskManagementAPIImpl implements TaskManagementAPI {

    private static final Logger LOGGER = Logger.getLogger(TaskManagementAPIImpl.class.getName());
    
    private final Connection connection;
    private final TaskInstanceService taskInstanceService;
    private final TaskCompletionService taskCompletionService;
    
    /**
     * Constructor with dependencies
     * 
     * @param connection Database connection
     */
    public TaskManagementAPIImpl(Connection connection) {
        this.connection = connection;
        this.taskInstanceService = new TaskInstanceService();
        this.taskCompletionService = new TaskCompletionService();
    }

    @Override
    public TaskInstance getTaskDetails(UUID taskId) {
        try {
            return taskInstanceService.getTaskInstance(connection, taskId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting task details", e);
            throw new RuntimeException("Error getting task details", e);
        }
    }

    @Override
    public List<TaskInstance> getTasksForWorkflow(UUID workflowInstanceId) {
        try {
            return taskInstanceService.getTasksForWorkflow(connection, workflowInstanceId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting tasks for workflow", e);
            throw new RuntimeException("Error getting tasks for workflow", e);
        }
    }

    @Override
    public boolean completeTask(UUID taskId, JsonNode outputJson) {
        try {
            return taskCompletionService.completeTask(connection, taskId, outputJson);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error completing task", e);
            throw new RuntimeException("Error completing task", e);
        }
    }

    @Override
    public boolean submitTask(UUID taskId, JsonNode outputJson) {
        try {
            return taskCompletionService.submitTask(connection, taskId, outputJson);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error submitting task", e);
            throw new RuntimeException("Error submitting task", e);
        }
    }

    @Override
    public boolean approveTask(UUID taskId, JsonNode outputJson) {
        try {
            return taskCompletionService.approveTask(connection, taskId, outputJson);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error approving task", e);
            throw new RuntimeException("Error approving task", e);
        }
    }

    @Override
    public boolean failTask(UUID taskId, String reason) {
        try {
            return taskCompletionService.failTask(connection, taskId, reason);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error failing task", e);
            throw new RuntimeException("Error failing task", e);
        }
    }

    @Override
    public TaskInstance updateTaskAssignment(UUID taskId, String newAssignee) {
        try {
            return taskInstanceService.updateTaskAssignment(connection, taskId, newAssignee);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating task assignment", e);
            throw new RuntimeException("Error updating task assignment", e);
        }
    }

    @Override
    public TaskInstance updateTaskDueDate(UUID taskId, LocalDateTime newDueDate) {
        try {
            return taskInstanceService.updateTaskDueDate(connection, taskId, newDueDate);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating task due date", e);
            throw new RuntimeException("Error updating task due date", e);
        }
    }

    @Override
    public boolean resubmitTask(UUID taskId) {
        try {
            TaskInstance task = getTaskDetails(taskId);
            
            if (task == null) {
                LOGGER.warning("Task not found: " + taskId);
                return false;
            }
            
            // Only failed tasks can be resubmitted
            if (task.getStatus() != TaskStatus.FAILED && task.getStatus() != TaskStatus.EXPIRED) {
                LOGGER.warning("Cannot resubmit task with status: " + task.getStatus());
                return false;
            }
            
            // Reset task status to NOT_STARTED
            TaskInstanceDAO taskInstDAO = new TaskInstanceDAO(connection);
            task.setStatus(TaskStatus.NOT_STARTED);
            task.setFailureReason(null);
            task.setStartTime(null);
            task.setEndTime(null);
            
            taskInstDAO.updateTaskInstance(task);
            
            // Queue for state management
            com.workday.pwe.execution.ExecutionQueuingInterceptor.queueForStateManagement(connection, task.getWorkflowInstanceId());
            
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error resubmitting task", e);
            throw new RuntimeException("Error resubmitting task", e);
        }
    }
}