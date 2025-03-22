package com.workday.pwe.handler;

import com.workday.pwe.dao.TaskGroupDefinitionDAO;
import com.workday.pwe.dao.TaskInstanceDAO;
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
 * Utility class for evaluating whether a task group has met its completion criteria.
 */
public class GroupCompletionEvaluator {
    
    private static final Logger LOGGER = Logger.getLogger(GroupCompletionEvaluator.class.getName());
    
    /**
     * Private constructor to prevent instantiation
     */
    private GroupCompletionEvaluator() {
        // Do not instantiate
    }
    
    /**
     * Evaluate if a task group has met its completion criteria
     * 
     * @param connection Database connection
     * @param groupInstance The task group instance to evaluate
     * @return true if the group is complete, false otherwise
     */
    public static boolean evaluateCompletion(Connection connection, TaskGroupInstance groupInstance) {
        try {
            TaskGroupDefinitionDAO groupDefDAO = new TaskGroupDefinitionDAO(connection);
            TaskInstanceDAO taskInstanceDAO = new TaskInstanceDAO(connection);
            
            TaskGroupDefinition groupDef = groupDefDAO.getTaskGroupDefinition(groupInstance.getTaskGroupDefId());
            List<TaskInstance> tasks = taskInstanceDAO.getTaskInstancesByGroupId(groupInstance.getId());
            
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
                
                if (isTerminalSuccessStatus(status)) {
                    completedTasks++;
                } else if (isTerminalFailureStatus(status)) {
                    failedTasks++;
                }
            }
            
            LOGGER.info("Evaluating group completion for group " + groupInstance.getId() + 
                       ": " + completedTasks + " completed, " + 
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
    
    /**
     * Check if a task is in a terminal success state
     * 
     * @param status The task status
     * @return true if the status is a terminal success state
     */
    public static boolean isTerminalSuccessStatus(TaskStatus status) {
        return status == TaskStatus.COMPLETED || 
               status == TaskStatus.SUBMITTED || 
               status == TaskStatus.APPROVED || 
               status == TaskStatus.REVIEWED || 
               status == TaskStatus.API_CALL_COMPLETE || 
               status == TaskStatus.SKIPPED;
    }
    
    /**
     * Check if a task is in a terminal failure state
     * 
     * @param status The task status
     * @return true if the status is a terminal failure state
     */
    public static boolean isTerminalFailureStatus(TaskStatus status) {
        return status == TaskStatus.FAILED || 
               status == TaskStatus.EXPIRED;
    }
    
    /**
     * Check if a task is in any terminal state
     * 
     * @param status The task status
     * @return true if the status is any terminal state
     */
    public static boolean isTerminalStatus(TaskStatus status) {
        return isTerminalSuccessStatus(status) || isTerminalFailureStatus(status);
    }
    
    /**
     * Check if a group has any failed tasks
     * 
     * @param connection Database connection
     * @param groupInstance The task group instance to check
     * @return true if any task has failed, false otherwise
     */
    public static boolean hasFailedTasks(Connection connection, TaskGroupInstance groupInstance) {
        try {
            TaskInstanceDAO taskInstanceDAO = new TaskInstanceDAO(connection);
            List<TaskInstance> tasks = taskInstanceDAO.getTaskInstancesByGroupId(groupInstance.getId());
            
            for (TaskInstance task : tasks) {
                if (isTerminalFailureStatus(task.getStatus())) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking for failed tasks", e);
            return false;
        }
    }
}