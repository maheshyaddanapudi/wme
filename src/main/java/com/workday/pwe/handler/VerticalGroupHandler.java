package com.workday.pwe.handler;

import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.model.TaskGroupDefinition;
import com.workday.pwe.model.TaskGroupInstance;
import com.workday.pwe.model.TaskInstance;

import java.sql.Connection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for vertical (sequential) task groups.
 */
public class VerticalGroupHandler extends TaskGroupHandler {

    private static final Logger LOGGER = Logger.getLogger(VerticalGroupHandler.class.getName());

    /**
     * Constructor with required parameters
     * 
     * @param connection Database connection
     * @param groupInstance The task group instance to handle
     */
    public VerticalGroupHandler(Connection connection, TaskGroupInstance groupInstance) {
        super(connection, groupInstance);
    }

    @Override
    public void execute() {
        try {
            TaskGroupDefinition groupDef = getTaskGroupDefinition();
            List<TaskInstance> tasks = getTasksInGroup();
            
            // Sort tasks by their order
            tasks.sort(Comparator.comparingInt(task -> {
                try {
                    return taskInstanceDAO.getTaskDefinition(task.getTaskDefId()).getTaskOrder();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error getting task order, defaulting to 0", e);
                    return 0;
                }
            }));
            
            LOGGER.info("Executing vertical group: " + groupInstance.getId() + 
                       " with " + tasks.size() + " tasks");
            
            // Check task status and start the next eligible task
            boolean taskStarted = false;
            
            for (TaskInstance task : tasks) {
                TaskStatus status = task.getStatus();
                
                if (status == TaskStatus.FAILED || status == TaskStatus.EXPIRED) {
                    // If any task fails, the vertical group fails
                    moveToFailed("Task " + task.getId() + " failed or expired");
                    return;
                } else if (status == TaskStatus.IN_PROGRESS || status == TaskStatus.BLOCKED) {
                    // There's already a task in progress, wait for it to complete
                    taskStarted = true;
                    break;
                } else if (status == TaskStatus.NOT_STARTED && !taskStarted) {
                    // Start the first not-started task we find
                    LOGGER.info("Starting task: " + task.getId() + " in vertical group");
                    startTask(task);
                    taskStarted = true;
                    break;
                } else if (status == TaskStatus.COMPLETED || 
                         status == TaskStatus.SUBMITTED || 
                         status == TaskStatus.APPROVED || 
                         status == TaskStatus.REVIEWED || 
                         status == TaskStatus.API_CALL_COMPLETE || 
                         status == TaskStatus.SKIPPED) {
                    // This task is already complete, continue to the next one
                    continue;
                }
            }
            
            // Check if the group is already complete
            if (evaluateCompletion()) {
                moveToComplete();
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing vertical group", e);
            moveToFailed("Error executing vertical group: " + e.getMessage());
        }
    }

    @Override
    public boolean evaluateCompletion() {
        try {
            List<TaskInstance> tasks = getTasksInGroup();
            
            if (tasks.isEmpty()) {
                // Empty groups are considered complete
                return true;
            }
            
            // For a vertical group, all tasks must be complete
            for (TaskInstance task : tasks) {
                TaskStatus status = task.getStatus();
                
                if (status != TaskStatus.COMPLETED && 
                    status != TaskStatus.SUBMITTED && 
                    status != TaskStatus.APPROVED && 
                    status != TaskStatus.REVIEWED && 
                    status != TaskStatus.API_CALL_COMPLETE && 
                    status != TaskStatus.SKIPPED) {
                    // Found a task that is not complete
                    return false;
                }
            }
            
            // All tasks are complete
            LOGGER.info("Vertical group is complete: " + groupInstance.getId());
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error evaluating group completion", e);
            return false;
        }
    }
    
    /**
     * Check if there are any failed tasks in the group
     * 
     * @return true if any task has failed, false otherwise
     */
    public boolean hasFailedTasks() {
        try {
            List<TaskInstance> tasks = getTasksInGroup();
            
            for (TaskInstance task : tasks) {
                if (task.getStatus() == TaskStatus.FAILED || task.getStatus() == TaskStatus.EXPIRED) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking for failed tasks", e);
            return false;
        }
    }
    
    /**
     * Get the next task that needs to be started
     * 
     * @return The next task to start, or empty if no task needs to be started
     */
    public Optional<TaskInstance> getNextTaskToStart() {
        try {
            TaskGroupDefinition groupDef = getTaskGroupDefinition();
            List<TaskInstance> tasks = getTasksInGroup();
            
            // Sort tasks by their order
            tasks.sort(Comparator.comparingInt(task -> {
                try {
                    return taskInstanceDAO.getTaskDefinition(task.getTaskDefId()).getTaskOrder();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error getting task order, defaulting to 0", e);
                    return 0;
                }
            }));
            
            boolean previousTasksComplete = true;
            
            for (TaskInstance task : tasks) {
                TaskStatus status = task.getStatus();
                
                if (previousTasksComplete && status == TaskStatus.NOT_STARTED) {
                    return Optional.of(task);
                }
                
                // Check if this task is complete before moving to the next
                previousTasksComplete = (status == TaskStatus.COMPLETED || 
                                       status == TaskStatus.SUBMITTED || 
                                       status == TaskStatus.APPROVED || 
                                       status == TaskStatus.REVIEWED || 
                                       status == TaskStatus.API_CALL_COMPLETE || 
                                       status == TaskStatus.SKIPPED);
                
                if (!previousTasksComplete) {
                    // Found an incomplete task that's not in NOT_STARTED state
                    // (it's either in progress or blocked)
                    break;
                }
            }
            
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting next task to start", e);
            return Optional.empty();
        }
    }
}