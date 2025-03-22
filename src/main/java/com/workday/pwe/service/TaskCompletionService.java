package com.workday.pwe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.workday.pwe.dao.TaskInstanceDAO;
import com.workday.pwe.dao.WorkflowHistoryDAO;
import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.enums.TaskType;
import com.workday.pwe.execution.ExecutionQueuingInterceptor;
import com.workday.pwe.handler.TaskHandler;
import com.workday.pwe.model.TaskInstance;
import com.workday.pwe.model.WorkflowHistory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for task completion operations.
 */
public class TaskCompletionService {

    private static final Logger LOGGER = Logger.getLogger(TaskCompletionService.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Complete a task with output data
     */
    public boolean completeTask(Connection connection, UUID taskId, JsonNode outputJson) throws SQLException {
        try {
            TaskInstanceDAO taskInstDAO = new TaskInstanceDAO(connection);
            TaskInstance taskInst = taskInstDAO.getTaskInstance(taskId);

            if (taskInst == null) {
                throw new IllegalArgumentException("Task instance not found: " + taskId);
            }

            // Check task is in a valid state to be completed
            if (taskInst.getStatus() != TaskStatus.IN_PROGRESS && taskInst.getStatus() != TaskStatus.NOT_STARTED) {
                throw new IllegalStateException("Task is not in a valid state to be completed. Current state: " +
                        taskInst.getStatus());
            }

            // Use TaskHandler to complete the task
            TaskHandler.completeAndClose(connection, taskInst, null);

            // Record history
            recordTaskCompletion(connection, taskInst, outputJson);

            // Queue for workflow state management
            ExecutionQueuingInterceptor.queueForStateManagement(connection, taskInst.getWorkflowInstanceId());

            LOGGER.info("Task completed: " + taskId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error completing task", e);
            throw new SQLException("Error completing task: " + e.getMessage(), e);
        }
    }

    /**
     * Submit a task with output data (for Submit tasks)
     *
     * @return
     */
    public boolean submitTask(Connection connection, UUID taskId, JsonNode outputJson) throws SQLException {
        try {
            TaskInstanceDAO taskInstDAO = new TaskInstanceDAO(connection);
            TaskInstance taskInst = taskInstDAO.getTaskInstance(taskId);

            if (taskInst == null) {
                throw new IllegalArgumentException("Task instance not found: " + taskId);
            }

            // Validate task type
            if (!isTaskOfType(connection, taskInst, TaskType.SUBMIT)) {
                throw new IllegalArgumentException("Task is not a Submit task");
            }

            // Update task status and output
            TaskStatus oldStatus = taskInst.getStatus();
            taskInst.setStatus(TaskStatus.SUBMITTED);
            taskInst.setOutputJson(outputJson);
            taskInst.setEndTime(LocalDateTime.now());

            taskInstDAO.updateTaskInstance(taskInst);

            // Record history
            recordStatusChange(connection, taskInst, oldStatus, TaskStatus.SUBMITTED);

            // Queue for workflow state management
            ExecutionQueuingInterceptor.queueForStateManagement(connection, taskInst.getWorkflowInstanceId());

            LOGGER.info("Task submitted: " + taskId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error submitting task", e);
            throw new SQLException("Error submitting task: " + e.getMessage(), e);
        }
    }

    /**
     * Approve a task with output data (for Approve tasks)
     *
     * @return
     */
    public boolean approveTask(Connection connection, UUID taskId, JsonNode outputJson) throws SQLException {
        try {
            TaskInstanceDAO taskInstDAO = new TaskInstanceDAO(connection);
            TaskInstance taskInst = taskInstDAO.getTaskInstance(taskId);

            if (taskInst == null) {
                throw new IllegalArgumentException("Task instance not found: " + taskId);
            }

            // Validate task type
            if (!isTaskOfType(connection, taskInst, TaskType.APPROVE)) {
                throw new IllegalArgumentException("Task is not an Approve task");
            }

            // Update task status and output
            TaskStatus oldStatus = taskInst.getStatus();
            taskInst.setStatus(TaskStatus.APPROVED);
            taskInst.setOutputJson(outputJson);
            taskInst.setEndTime(LocalDateTime.now());

            taskInstDAO.updateTaskInstance(taskInst);

            // Record history
            recordStatusChange(connection, taskInst, oldStatus, TaskStatus.APPROVED);

            // Queue for workflow state management
            ExecutionQueuingInterceptor.queueForStateManagement(connection, taskInst.getWorkflowInstanceId());

            LOGGER.info("Task approved: " + taskId);

            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error approving task", e);
            throw new SQLException("Error approving task: " + e.getMessage(), e);
        }
    }

    /**
     * Review a task with output data (for Review tasks)
     */
    public void reviewTask(Connection connection, UUID taskId, JsonNode outputJson) throws SQLException {
        try {
            TaskInstanceDAO taskInstDAO = new TaskInstanceDAO(connection);
            TaskInstance taskInst = taskInstDAO.getTaskInstance(taskId);

            if (taskInst == null) {
                throw new IllegalArgumentException("Task instance not found: " + taskId);
            }

            // Validate task type
            if (!isTaskOfType(connection, taskInst, TaskType.REVIEW)) {
                throw new IllegalArgumentException("Task is not a Review task");
            }

            // Update task status and output
            TaskStatus oldStatus = taskInst.getStatus();
            taskInst.setStatus(TaskStatus.REVIEWED);
            taskInst.setOutputJson(outputJson);
            taskInst.setEndTime(LocalDateTime.now());

            taskInstDAO.updateTaskInstance(taskInst);

            // Record history
            recordStatusChange(connection, taskInst, oldStatus, TaskStatus.REVIEWED);

            // Queue for workflow state management
            ExecutionQueuingInterceptor.queueForStateManagement(connection, taskInst.getWorkflowInstanceId());

            LOGGER.info("Task reviewed: " + taskId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reviewing task", e);
            throw new SQLException("Error reviewing task: " + e.getMessage(), e);
        }
    }

    /**
     * Fail a task with a reason
     */
    public boolean failTask(Connection connection, UUID taskId, String failureReason) throws SQLException {
        try {
            TaskInstanceDAO taskInstDAO = new TaskInstanceDAO(connection);
            TaskInstance taskInst = taskInstDAO.getTaskInstance(taskId);

            if (taskInst == null) {
                throw new IllegalArgumentException("Task instance not found: " + taskId);
            }

            // Update task status, failure reason, and end time
            TaskStatus oldStatus = taskInst.getStatus();
            taskInst.setStatus(TaskStatus.FAILED);
            taskInst.setFailureReason(failureReason);
            taskInst.setEndTime(LocalDateTime.now());

            taskInstDAO.updateTaskInstance(taskInst);

            // Record history
            recordStatusChange(connection, taskInst, oldStatus, TaskStatus.FAILED);

            // Queue for workflow state management
            ExecutionQueuingInterceptor.queueForStateManagement(connection, taskInst.getWorkflowInstanceId());

            LOGGER.info("Task failed: " + taskId + ", Reason: " + failureReason);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error failing task", e);
            throw new SQLException("Error failing task: " + e.getMessage(), e);
        }
    }

    /**
     * Skip a task
     */
    public void skipTask(Connection connection, UUID taskId, String reason) throws SQLException {
        try {
            TaskInstanceDAO taskInstDAO = new TaskInstanceDAO(connection);
            TaskInstance taskInst = taskInstDAO.getTaskInstance(taskId);

            if (taskInst == null) {
                throw new IllegalArgumentException("Task instance not found: " + taskId);
            }

            // Update task status and end time
            TaskStatus oldStatus = taskInst.getStatus();
            taskInst.setStatus(TaskStatus.SKIPPED);
            taskInst.setEndTime(LocalDateTime.now());

            // Add skip reason to output JSON
            ObjectNode outputJson = OBJECT_MAPPER.createObjectNode();
            outputJson.put("skipped", true);
            outputJson.put("reason", reason);
            taskInst.setOutputJson(outputJson);

            taskInstDAO.updateTaskInstance(taskInst);

            // Record history
            recordStatusChange(connection, taskInst, oldStatus, TaskStatus.SKIPPED);

            // Queue for workflow state management
            ExecutionQueuingInterceptor.queueForStateManagement(connection, taskInst.getWorkflowInstanceId());

            LOGGER.info("Task skipped: " + taskId + ", Reason: " + reason);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error skipping task", e);
            throw new SQLException("Error skipping task: " + e.getMessage(), e);
        }
    }

    // Private helper methods

    private boolean isTaskOfType(Connection connection, TaskInstance taskInst, TaskType taskType) throws SQLException {
        TaskInstanceDAO taskInstDAO = new TaskInstanceDAO(connection);
        var taskDef = taskInstDAO.getTaskDefinition(taskInst.getTaskDefId());
        return taskDef.getTaskType() == taskType;
    }

    private void recordTaskCompletion(Connection connection, TaskInstance taskInst,
                                      JsonNode outputJson) throws SQLException {
        try {
            WorkflowHistoryDAO historyDAO = new WorkflowHistoryDAO(connection);

            ObjectNode detailsJson = OBJECT_MAPPER.createObjectNode();
            detailsJson.put("oldStatus", taskInst.getStatus().name());
            detailsJson.put("newStatus", TaskStatus.COMPLETED.name());
            if (outputJson != null) {
                detailsJson.set("hasOutput", OBJECT_MAPPER.createObjectNode().put("value", true));
            }

            WorkflowHistory history = new WorkflowHistory(
                    taskInst.getWorkflowInstanceId(),
                    "TASK",
                    taskInst.getId(),
                    "COMPLETION",
                    detailsJson
            );

            historyDAO.addHistoryRecord(history);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error recording task completion history", e);
        }
    }

    private void recordStatusChange(Connection connection, TaskInstance taskInst,
                                    TaskStatus oldStatus, TaskStatus newStatus) throws SQLException {
        try {
            WorkflowHistoryDAO historyDAO = new WorkflowHistoryDAO(connection);

            ObjectNode detailsJson = OBJECT_MAPPER.createObjectNode();
            detailsJson.put("oldStatus", oldStatus.name());
            detailsJson.put("newStatus", newStatus.name());

            WorkflowHistory history = new WorkflowHistory(
                    taskInst.getWorkflowInstanceId(),
                    "TASK",
                    taskInst.getId(),
                    "STATUS_CHANGE",
                    detailsJson
            );

            historyDAO.addHistoryRecord(history);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error recording task status change history", e);
        }
    }
}