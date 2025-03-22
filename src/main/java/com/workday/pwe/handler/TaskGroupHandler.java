package com.workday.pwe.handler;

import com.workday.pwe.dao.TaskGroupDefinitionDAO;
import com.workday.pwe.dao.TaskGroupInstanceDAO;
import com.workday.pwe.dao.TaskInstanceDAO;
import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.model.TaskGroupDefinition;
import com.workday.pwe.model.TaskGroupInstance;
import com.workday.pwe.model.TaskInstance;

import java.sql.Connection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for task group handlers.
 */
public abstract class TaskGroupHandler {

    private static final Logger LOGGER = Logger.getLogger(TaskGroupHandler.class.getName());
    
    protected final TaskGroupInstance groupInstance;
    protected final Connection connection;
    protected final TaskGroupInstanceDAO groupInstanceDAO;
    protected final TaskInstanceDAO taskInstanceDAO;
    protected final TaskGroupDefinitionDAO groupDefinitionDAO;
    
    /**
     * Constructor with required parameters
     * 
     * @param connection Database connection
     * @param groupInstance The task group instance to handle
     */
    protected TaskGroupHandler(Connection connection, TaskGroupInstance groupInstance) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        if (groupInstance == null) {
            throw new IllegalArgumentException("TaskGroupInstance cannot be null");
        }
        
        this.connection = connection;
        this.groupInstance = groupInstance;
        this.groupInstanceDAO = new TaskGroupInstanceDAO(connection);
        this.taskInstanceDAO = new TaskInstanceDAO(connection);
        this.groupDefinitionDAO = new TaskGroupDefinitionDAO(connection);
    }
    
    /**
     * Update the task group status to IN_PROGRESS
     */
    protected void moveToInProgress() {
        try {
            groupInstance.setStatus(TaskStatus.IN_PROGRESS);
            groupInstanceDAO.updateTaskGroupInstance(groupInstance);
            LOGGER.info("Task group moved to IN_PROGRESS: " + groupInstance.getId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating task group to IN_PROGRESS", e);
            throw new RuntimeException("Error updating task group to IN_PROGRESS", e);
        }
    }
    
    /**
     * Update the task group status to COMPLETED
     */
    protected void moveToComplete() {
        try {
            groupInstance.setStatus(TaskStatus.COMPLETED);
            groupInstanceDAO.updateTaskGroupInstance(groupInstance);
            LOGGER.info("Task group completed successfully: " + groupInstance.getId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating task group completion state", e);
            throw new RuntimeException("Error updating task group completion state", e);
        }
    }
    
    /**
     * Update the task group status to FAILED
     * 
     * @param reason Reason for failure
     */
    protected void moveToFailed(String reason) {
        try {
            groupInstance.setStatus(TaskStatus.FAILED);
            groupInstanceDAO.updateTaskGroupInstance(groupInstance, reason);
            LOGGER.warning("Task group failed: " + groupInstance.getId() + ", Reason: " + reason);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating task group to FAILED", e);
            throw new RuntimeException("Error updating task group to FAILED", e);
        }
    }
    
    /**
     * Get the task group definition
     * 
     * @return The task group definition
     */
    protected TaskGroupDefinition getTaskGroupDefinition() {
        try {
            return groupDefinitionDAO.getTaskGroupDefinition(groupInstance.getTaskGroupDefId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving task group definition", e);
            throw new RuntimeException("Error retrieving task group definition", e);
        }
    }
    
    /**
     * Get all tasks in this group
     * 
     * @return List of task instances in this group
     */
    protected List<TaskInstance> getTasksInGroup() {
        try {
            return taskInstanceDAO.getTaskInstancesByGroupId(groupInstance.getId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving tasks in group", e);
            throw new RuntimeException("Error retrieving tasks in group", e);
        }
    }
    
    /**
     * Start the execution of a task
     * 
     * @param taskInstance The task to execute
     */
    protected void startTask(TaskInstance taskInstance) {
        try {
            TaskHandler.run(connection, taskInstance);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error starting task", e);
            throw new RuntimeException("Error starting task", e);
        }
    }
    
    /**
     * Execute the task group
     * Must be implemented by concrete subclasses
     */
    public abstract void execute();
    
    /**
     * Check if the group has met its completion criteria
     * 
     * @return true if the group is complete, false otherwise
     */
    public abstract boolean evaluateCompletion();
    
    /**
     * Factory method to get the appropriate handler based on group type
     * 
     * @param connection Database connection
     * @param groupInstance The task group instance
     * @return The appropriate task group handler
     */
    public static TaskGroupHandler getHandler(Connection connection, TaskGroupInstance groupInstance) {
        try {
            TaskGroupDefinitionDAO groupDefDAO = new TaskGroupDefinitionDAO(connection);
            TaskGroupDefinition groupDef = groupDefDAO.getTaskGroupDefinition(groupInstance.getTaskGroupDefId());
            
            if (groupDef == null) {
                throw new IllegalArgumentException("Group definition not found for group instance: " + groupInstance.getId());
            }
            
            switch (groupDef.getGroupType()) {
                case HORIZONTAL:
                    return new HorizontalGroupHandler(connection, groupInstance);
                case VERTICAL:
                    return new VerticalGroupHandler(connection, groupInstance);
                default:
                    throw new IllegalArgumentException("Unknown group type: " + groupDef.getGroupType());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating task group handler", e);
            throw new RuntimeException("Error creating task group handler", e);
        }
    }
    
    /**
     * Run the task group
     */
    public void run() {
        try {
            moveToInProgress();
            execute();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error running task group", e);
            moveToFailed("Error running task group: " + e.getMessage());
        }
    }
}