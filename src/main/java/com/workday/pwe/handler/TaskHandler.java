package com.workday.pwe.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.workday.pwe.dao.TaskDAO;
import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.execution.ExecutionQueuingInterceptor;
import com.workday.pwe.model.TaskInstance;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Abstract class representing a task handler in a workflow execution system.
 * Handles task execution lifecycle and transitions.
 */
public abstract class TaskHandler {

    private static final Logger LOGGER = Logger.getLogger(TaskHandler.class.getName());

    private final TaskInstance taskInstance;
    private Connection connection; // Connection will be set using setter method if the task type is stateful. 

    /**
     * Constructor that initializes the handler with a task instance
     * 
     * @param taskInstance The task instance to be handled
     */
    protected TaskHandler(TaskInstance taskInstance) {
        if (taskInstance == null) {
            throw new IllegalArgumentException("TaskInstance cannot be null");
        }
        this.taskInstance = taskInstance;
    }

    /**
     * Indicates if this handler maintains state across method calls
     * 
     * @return true if stateful, false otherwise
     */
    protected boolean isStateful() {
        return false;
    }

    /**
     * Sets the database connection for this handler
     * 
     * @param connection The SQL connection to use
     */
    protected void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * Updates the task status to IN_PROGRESS
     * 
     * @param connection Database connection
     */
    private final void moveToInProgress(Connection connection) {
        try {
            new TaskDAO(connection).updateTask(taskInstance.getId(), TaskStatus.IN_PROGRESS);
            LOGGER.info("Task moved to IN_PROGRESS: " + taskInstance.getId());
        } catch (Exception e) {
            throw new RuntimeException("Error updating task to IN_PROGRESS", e);
        }
    }

    /**
     * Updates the task to a completion state
     * 
     * @param connection Database connection
     * @param reasonForFailure Optional failure reason, null for success
     */
    private final void moveToComplete(Connection connection, String reasonForFailure) {
        try {
            TaskDAO taskDAO = new TaskDAO(connection);
            if (reasonForFailure == null) {
                taskDAO.updateTask(taskInstance.getId(), getCompletionStatus());
                LOGGER.info("Task completed successfully: " + taskInstance.getId());
            } else {
                taskDAO.updateTask(taskInstance.getId(), getFailureStatus(), reasonForFailure);
                LOGGER.warning("Task failed: " + taskInstance.getId() + ", Reason: " + reasonForFailure);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error updating task completion state", e);
        }
    }

    /**
     * Prepare method for task setup before execution
     * Can be overridden by subclasses
     * 
     * @param connection Database connection
     */
    protected void prepare(Connection connection) {
        // Default empty implementation
    }

    /**
     * Cleanup method for task cleanup after execution
     * Can be overridden by subclasses
     * 
     * @param connection Database connection
     */
    protected void cleanup(Connection connection) { 
        // Default empty implementation
    }

    /**
     * Execute the task-specific logic
     * Must be implemented by subclasses
     * 
     * @param connection Database connection
     */
    protected abstract void execute(Connection connection);

    /**
     * Get the task type identifier
     * Must be implemented by subclasses
     * 
     * @return The task type as a string
     */
    protected abstract String getTaskType();

    /**
     * Get the completion status for successful task execution
     * Can be overridden by subclasses
     * 
     * @return The task status to use on successful completion
     */
    protected TaskStatus getCompletionStatus() {
        return TaskStatus.COMPLETED;
    }

    /**
     * Get the failure status for failed task execution
     * Can be overridden by subclasses
     * 
     * @return The task status to use on failure
     */
    protected TaskStatus getFailureStatus() {
        return TaskStatus.FAILED;
    }

    /**
     * Static method to run a task
     * 
     * @param connection Database connection
     * @param taskInstance The task instance to run
     */
    public static void run(Connection connection, TaskInstance taskInstance) {
        TaskHandler handler = TaskHandlerRegistry.getHandler(connection, taskInstance);
        handler.prepare(connection);
        handler.moveToInProgress(connection);
        handler.execute(handler.getConnection());
    }

    /**
     * Static method to complete and close a task
     * 
     * @param connection Database connection
     * @param taskInstance The task instance to complete
     * @param reasonForFailure Optional failure reason, null for success
     */
    public static void completeAndClose(Connection connection, TaskInstance taskInstance, String reasonForFailure) {
        TaskHandler handler = TaskHandlerRegistry.getHandler(connection, taskInstance);
        handler.moveToComplete(connection, reasonForFailure);
        handler.cleanup(connection);
        handler.close(connection);
    }

    /**
     * Close the task and queue for state management
     * 
     * @param connection Database connection
     */
    private final void close(Connection connection) {
        List<String> workflowExecutionIds = Collections.singletonList(taskInstance.getWorkflowInstanceId().toString());
        ExecutionQueuingInterceptor.queueForStateManagement(connection, workflowExecutionIds);
        LOGGER.info("Task closed and queued: " + taskInstance.getId());
    }

    /**
     * Get the database connection
     * 
     * @return Database connection
     */
    protected Connection getConnection() {
        return this.connection;
    }

    /**
     * Get the task instance being handled
     * 
     * @return The task instance
     */
    protected TaskInstance getTaskInstance() {
        return this.taskInstance;
    }
    
    /**
     * Complete the task with output data
     * 
     * @param connection Database connection
     * @param outputJson The output data
     */
    protected void completeTask(Connection connection, JsonNode outputJson) {
        taskInstance.setOutputJson(outputJson);
        completeAndClose(connection, taskInstance, null);
    }
    
    /**
     * Fail the task with a reason
     * 
     * @param connection Database connection
     * @param failureReason The reason for failure
     */
    protected void failTask(Connection connection, String failureReason) {
        completeAndClose(connection, taskInstance, failureReason);
    }
}