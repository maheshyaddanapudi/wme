package com.workday.pwe.handler;

import com.workday.pwe.enums.CompletionCriteria;
import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.model.TaskGroupDefinition;
import com.workday.pwe.model.TaskGroupInstance;
import com.workday.pwe.model.TaskInstance;

import java.sql.Connection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for horizontal (parallel) task groups.
 */
public class HorizontalGroupHandler extends TaskGroupHandler {

    private static final Logger LOGGER = Logger.getLogger(HorizontalGroupHandler.class.getName());

    /**
     * Constructor with required parameters
     * 
     * @param connection Database connection
     * @param groupInstance The task group instance to handle
     */
    public HorizontalGroupHandler(Connection connection, TaskGroupInstance groupInstance) {
        super(connection, groupInstance);
    }

    @Override
    public void execute() {
        try {
            TaskGroupDefinition groupDef = getTaskGroupDefinition();
            List<TaskInstance> tasks = getTasksInGroup();
            
            LOGGER.info("Executing horizontal group: " + groupInstance.getId() + 
                       " with " + tasks.size() + " tasks");
            
            // In a horizontal group, all tasks are started in parallel
            for (TaskInstance task : tasks) {
                if (task.getStatus() == TaskStatus.NOT_STARTED) {
                    LOGGER.info("Starting task: " + task.getId() + " in horizontal group");
                    startTask(task);
                }
            }
            
            // Check if the group is already complete
            if (evaluateCompletion()) {
                moveToComplete();
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing horizontal group", e);
            moveToFailed("Error executing horizontal group: " + e.getMessage());
        }
    }

    @Override
    public boolean evaluateCompletion() {
        try {
            TaskGroupDefinition groupDef = getTaskGroupDefinition();
            List<TaskInstance> tasks = getTasksInGroup();
            
            if (tasks.isEmpty()) {
                // Empty groups are considered complete
                return true;
            }
            
            CompletionCriteria criteria = groupDef.getCompletionCriteria();
            int totalTasks = tasks.size();
            int completedTasks = 0;
            int failedTasks = 0;
            
            for (TaskInstance task : tasks) {
                TaskStatus status = task.getStatus();
                
                if (status == TaskStatus.COMPLETED || 
                    status == TaskStatus.SUBMITTED || 
                    status == TaskStatus.APPROVED || 
                    status == TaskStatus.REVIEWED || 
                    status == TaskStatus.API_CALL_COMPLETE || 
                    status == TaskStatus.SKIPPED) {
                    completedTasks++;
                } else if (status == TaskStatus.FAILED || status == TaskStatus.EXPIRED) {
                    failedTasks++;
                }
            }
            
            LOGGER.info("Evaluating horizontal group completion: " + 
                       completedTasks + " completed, " + 
                       failedTasks + " failed out of " + 
                       totalTasks + " total tasks");
            
            switch (criteria) {
                case ALL:
                    // All tasks must complete successfully
                    return completedTasks == totalTasks;
                    
                case ANY:
                    // Any one task must complete successfully
                    return completedTasks > 0;
                    
                case N_OF_M:
                    // N of M tasks must complete successfully
                    int minRequired = groupInstance.getMinCompletion();
                    return completedTasks >= minRequired;
                    
                default:
                    LOGGER.warning("Unknown completion criteria: " + criteria);
                    return false;
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error evaluating group completion", e);
            return false;
        }
    }
}