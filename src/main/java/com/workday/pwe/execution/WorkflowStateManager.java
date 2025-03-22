package com.workday.pwe.execution;

import com.workday.pwe.dao.TaskGroupInstanceDAO;
import com.workday.pwe.dao.TaskInstanceDAO;
import com.workday.pwe.dao.WorkflowExecutionQueueDAO;
import com.workday.pwe.dao.WorkflowInstanceDAO;
import com.workday.pwe.enums.QueueStatus;
import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.enums.WorkflowStatus;
import com.workday.pwe.handler.GroupCompletionEvaluator;
import com.workday.pwe.handler.TaskGroupHandler;
import com.workday.pwe.handler.TaskHandler;
import com.workday.pwe.model.TaskGroupInstance;
import com.workday.pwe.model.TaskInstance;
import com.workday.pwe.model.WorkflowInstance;

import java.sql.Connection;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.UUID;

/**
 * Manages the state transitions of workflows and tasks.
 * This is a core component that determines what happens next in a workflow execution.
 */
public class WorkflowStateManager {

    private static final Logger LOGGER = Logger.getLogger(WorkflowStateManager.class.getName());

    /**
     * Private constructor to prevent instantiation
     */
    private WorkflowStateManager() {
        // Do not instantiate
    }

    /**
     * Main decision method that processes a workflow execution
     * 
     * @param workflowId The workflow instance ID to process
     * @param connection Database connection
     * @param lastPollTime The last time the workflow was processed (can be null)
     */
    public static void decide(String workflowId, Connection connection, Timestamp lastPollTime) {
        WorkflowExecutionQueueDAO queueDAO = new WorkflowExecutionQueueDAO(connection);
        WorkflowInstanceDAO workflowDAO = new WorkflowInstanceDAO(connection);
        TaskGroupInstanceDAO groupDAO = new TaskGroupInstanceDAO(connection);
        TaskInstanceDAO taskDAO = new TaskInstanceDAO(connection);

        try {
            // Update queue status to PROCESSING
            queueDAO.updateQueueStatus(UUID.fromString(workflowId), QueueStatus.PROCESSING);
            
            // Check if workflow is still in progress
            WorkflowInstance workflow = workflowDAO.getWorkflowInstance(UUID.fromString(workflowId));
            if (workflow == null || workflow.getStatus() != WorkflowStatus.RUNNING) {
                // Workflow is not runnable, remove from queue
                queueDAO.removeFromQueue(UUID.fromString(workflowId));
                LOGGER.info("Workflow not in RUNNING state, removed from queue: " + workflowId);
                return;
            }

            // Get completed tasks/groups since last poll
            List<TaskInstance> completedTasks;
            List<TaskGroupInstance> completedGroups;
            
            if (lastPollTime != null) {
                completedTasks = taskDAO.getCompletedTasksAfter(UUID.fromString(workflowId), lastPollTime);
                completedGroups = groupDAO.getCompletedGroupsAfter(UUID.fromString(workflowId), lastPollTime);
            } else {
                completedTasks = taskDAO.getCompletedTasks(UUID.fromString(workflowId));
                completedGroups = groupDAO.getCompletedGroups(UUID.fromString(workflowId));
            }

            LOGGER.info("Found " + completedTasks.size() + " completed tasks and " + 
                       completedGroups.size() + " completed groups for workflow: " + workflowId);

            // Process task groups first
            for (TaskGroupInstance group : completedGroups) {
                processTaskGroup(connection, group);
            }

            // Process individual tasks not in groups
            for (TaskInstance task : completedTasks) {
                if (task.getTaskGroupInstanceId() == null) {
                    processTask(connection, task);
                }
            }

            // Now check if we need to start new tasks or groups
            startEligibleTasksAndGroups(connection, workflow);

            // Check if the workflow is complete
            checkWorkflowCompletion(connection, workflow);

            // Remove from queue if done processing
            queueDAO.removeFromQueue(UUID.fromString(workflowId));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing workflow: " + workflowId, e);
            
            // Mark as FAILED if there was an error
            try {
                workflowDAO.updateWorkflowStatus(UUID.fromString(workflowId), WorkflowStatus.FAILED);
                queueDAO.removeFromQueue(UUID.fromString(workflowId));
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error updating workflow status after error", ex);
            }
        }
    }

    /**
     * Process a completed task
     * 
     * @param connection Database connection
     * @param task The completed task
     */
    private static void processTask(Connection connection, TaskInstance task) {
        try {
            LOGGER.info("Processing completed task: " + task.getId());
            
            // If task is part of a group, check if the group is complete
            if (task.getTaskGroupInstanceId() != null) {
                TaskGroupInstanceDAO groupDAO = new TaskGroupInstanceDAO(connection);
                TaskGroupInstance group = groupDAO.getTaskGroupInstance(task.getTaskGroupInstanceId());
                
                if (group != null) {
                    boolean isComplete = GroupCompletionEvaluator.evaluateCompletion(connection, group);
                    if (isComplete) {
                        groupDAO.updateTaskGroupStatus(group.getId(), TaskStatus.COMPLETED);
                        LOGGER.info("Task group completed: " + group.getId());
                        
                        // Queue for state management to process the completed group
                        ExecutionQueuingInterceptor.queueForStateManagement(connection, task.getWorkflowInstanceId());
                    }
                }
            }
            
            // If task is a terminal failure status, check if we need to fail the workflow or group
            if (GroupCompletionEvaluator.isTerminalFailureStatus(task.getStatus())) {
                handleTaskFailure(connection, task);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing task: " + task.getId(), e);
        }
    }

    /**
     * Process a completed task group
     * 
     * @param connection Database connection
     * @param group The completed task group
     */
    private static void processTaskGroup(Connection connection, TaskGroupInstance group) {
        try {
            LOGGER.info("Processing completed group: " + group.getId());
            
            // If group is part of a parent group, check if the parent is complete
            if (group.getParentGroupInstId() != null) {
                TaskGroupInstanceDAO groupDAO = new TaskGroupInstanceDAO(connection);
                TaskGroupInstance parentGroup = groupDAO.getTaskGroupInstance(group.getParentGroupInstId());
                
                if (parentGroup != null) {
                    boolean isComplete = GroupCompletionEvaluator.evaluateCompletion(connection, parentGroup);
                    if (isComplete) {
                        groupDAO.updateTaskGroupStatus(parentGroup.getId(), TaskStatus.COMPLETED);
                        LOGGER.info("Parent task group completed: " + parentGroup.getId());
                        
                        // Queue for state management to process the completed parent group
                        ExecutionQueuingInterceptor.queueForStateManagement(connection, group.getWorkflowInstanceId());
                    }
                }
            }
            
            // If group is a terminal failure status, handle failure
            if (group.getStatus() == TaskStatus.FAILED) {
                handleGroupFailure(connection, group);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing group: " + group.getId(), e);
        }
    }

    /**
     * Handle a task failure
     * 
     * @param connection Database connection
     * @param task The failed task
     */
    private static void handleTaskFailure(Connection connection, TaskInstance task) {
        try {
            LOGGER.warning("Handling task failure: " + task.getId());
            
            // If task is part of a group, check the group's completion criteria
            if (task.getTaskGroupInstanceId() != null) {
                TaskGroupInstanceDAO groupDAO = new TaskGroupInstanceDAO(connection);
                TaskGroupInstance group = groupDAO.getTaskGroupInstance(task.getTaskGroupInstanceId());
                
                if (group != null) {
                    // For vertical groups, a single failure fails the group
                    if (groupDAO.isVerticalGroup(group.getTaskGroupDefId())) {
                        groupDAO.updateTaskGroupStatus(group.getId(), TaskStatus.FAILED);
                        LOGGER.warning("Vertical task group failed due to task failure: " + group.getId());
                        
                        // Queue for state management to process the failed group
                        ExecutionQueuingInterceptor.queueForStateManagement(connection, task.getWorkflowInstanceId());
                    } else {
                        // For horizontal groups, check if failure violates completion criteria
                        boolean canStillComplete = canGroupStillComplete(connection, group);
                        if (!canStillComplete) {
                            groupDAO.updateTaskGroupStatus(group.getId(), TaskStatus.FAILED);
                            LOGGER.warning("Horizontal task group failed due to task failure: " + group.getId());
                            
                            // Queue for state management to process the failed group
                            ExecutionQueuingInterceptor.queueForStateManagement(connection, task.getWorkflowInstanceId());
                        }
                    }
                }
            } else {
                // Task is not part of a group, check workflow level failure policy
                // For now, we'll fail the workflow if any top-level task fails
                WorkflowInstanceDAO workflowDAO = new WorkflowInstanceDAO(connection);
                workflowDAO.updateWorkflowStatus(task.getWorkflowInstanceId(), WorkflowStatus.FAILED);
                LOGGER.warning("Workflow failed due to task failure: " + task.getWorkflowInstanceId());
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling task failure: " + task.getId(), e);
        }
    }

    /**
     * Handle a group failure
     * 
     * @param connection Database connection
     * @param group The failed group
     */
    private static void handleGroupFailure(Connection connection, TaskGroupInstance group) {
        try {
            LOGGER.warning("Handling group failure: " + group.getId());
            
            // If group is part of a parent group, check the parent's completion criteria
            if (group.getParentGroupInstId() != null) {
                TaskGroupInstanceDAO groupDAO = new TaskGroupInstanceDAO(connection);
                TaskGroupInstance parentGroup = groupDAO.getTaskGroupInstance(group.getParentGroupInstId());
                
                if (parentGroup != null) {
                    // For vertical groups, a single failure fails the group
                    if (groupDAO.isVerticalGroup(parentGroup.getTaskGroupDefId())) {
                        groupDAO.updateTaskGroupStatus(parentGroup.getId(), TaskStatus.FAILED);
                        LOGGER.warning("Vertical parent task group failed due to child group failure: " + parentGroup.getId());
                        
                        // Queue for state management to process the failed parent group
                        ExecutionQueuingInterceptor.queueForStateManagement(connection, group.getWorkflowInstanceId());
                    } else {
                        // For horizontal groups, check if failure violates completion criteria
                        boolean canStillComplete = canGroupStillComplete(connection, parentGroup);
                        if (!canStillComplete) {
                            groupDAO.updateTaskGroupStatus(parentGroup.getId(), TaskStatus.FAILED);
                            LOGGER.warning("Horizontal parent task group failed due to child group failure: " + parentGroup.getId());
                            
                            // Queue for state management to process the failed parent group
                            ExecutionQueuingInterceptor.queueForStateManagement(connection, group.getWorkflowInstanceId());
                        }
                    }
                }
            } else {
                // Group is not part of a parent group, check workflow level failure policy
                // For now, we'll fail the workflow if any top-level group fails
                WorkflowInstanceDAO workflowDAO = new WorkflowInstanceDAO(connection);
                workflowDAO.updateWorkflowStatus(group.getWorkflowInstanceId(), WorkflowStatus.FAILED);
                LOGGER.warning("Workflow failed due to group failure: " + group.getWorkflowInstanceId());
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling group failure: " + group.getId(), e);
        }
    }

    /**
     * Check if a group can still meet its completion criteria despite failures
     * 
     * @param connection Database connection
     * @param group The group to check
     * @return true if the group can still complete, false otherwise
     */
    private static boolean canGroupStillComplete(Connection connection, TaskGroupInstance group) {
        try {
            TaskGroupInstanceDAO groupDAO = new TaskGroupInstanceDAO(connection);
            TaskInstanceDAO taskDAO = new TaskInstanceDAO(connection);
            
            // For horizontal groups with ANY criteria, we just need one success
            if (groupDAO.hasCompletionCriteria(group.getTaskGroupDefId(), "ANY")) {
                List<TaskInstance> tasks = taskDAO.getTaskInstancesByGroupId(group.getId());
                
                // Count tasks that are already successful or could still succeed
                int possibleSuccesses = 0;
                for (TaskInstance task : tasks) {
                    if (GroupCompletionEvaluator.isTerminalSuccessStatus(task.getStatus())) {
                        possibleSuccesses++;
                    } else if (!GroupCompletionEvaluator.isTerminalFailureStatus(task.getStatus())) {
                        possibleSuccesses++;
                    }
                }
                
                return possibleSuccesses > 0;
            }
            
            // For horizontal groups with N_OF_M criteria, we need at least N successes
            if (groupDAO.hasCompletionCriteria(group.getTaskGroupDefId(), "N_OF_M")) {
                List<TaskInstance> tasks = taskDAO.getTaskInstancesByGroupId(group.getId());
                int minRequired = group.getMinCompletion();
                
                // Count tasks that are already successful or could still succeed
                int possibleSuccesses = 0;
                for (TaskInstance task : tasks) {
                    if (GroupCompletionEvaluator.isTerminalSuccessStatus(task.getStatus())) {
                        possibleSuccesses++;
                    } else if (!GroupCompletionEvaluator.isTerminalFailureStatus(task.getStatus())) {
                        possibleSuccesses++;
                    }
                }
                
                return possibleSuccesses >= minRequired;
            }
            
            // For ALL criteria, all tasks must succeed
            List<TaskInstance> tasks = taskDAO.getTaskInstancesByGroupId(group.getId());
            
            // If any task has already failed, the group cannot complete
            for (TaskInstance task : tasks) {
                if (GroupCompletionEvaluator.isTerminalFailureStatus(task.getStatus())) {
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking if group can still complete: " + group.getId(), e);
            return false;
        }
    }

    /**
     * Start eligible tasks and groups in a workflow
     * 
     * @param connection Database connection
     * @param workflow The workflow instance
     */
    private static void startEligibleTasksAndGroups(Connection connection, WorkflowInstance workflow) {
        try {
            LOGGER.info("Starting eligible tasks and groups for workflow: " + workflow.getId());
            
            TaskGroupInstanceDAO groupDAO = new TaskGroupInstanceDAO(connection);
            TaskInstanceDAO taskDAO = new TaskInstanceDAO(connection);
            
            // First, get all root task groups (those without a parent)
            List<TaskGroupInstance> rootGroups = groupDAO.getRootTaskGroups(workflow.getId());
            
            // Start any root groups that are not yet started
            for (TaskGroupInstance group : rootGroups) {
                if (group.getStatus() == TaskStatus.NOT_STARTED) {
                    TaskGroupHandler handler = TaskGroupHandler.getHandler(connection, group);
                    handler.run();
                }
            }
            
            // Now, get all top-level tasks (those not in any group)
            List<TaskInstance> topLevelTasks = taskDAO.getTopLevelTasks(workflow.getId());
            
            // Start any top-level tasks that are not yet started
            for (TaskInstance task : topLevelTasks) {
                if (task.getStatus() == TaskStatus.NOT_STARTED) {
                    TaskHandler.run(connection, task);
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error starting eligible tasks and groups: " + workflow.getId(), e);
        }
    }

    /**
     * Check if a workflow is complete
     * 
     * @param connection Database connection
     * @param workflow The workflow instance to check
     */
    private static void checkWorkflowCompletion(Connection connection, WorkflowInstance workflow) {
        try {
            LOGGER.info("Checking if workflow is complete: " + workflow.getId());
            
            TaskGroupInstanceDAO groupDAO = new TaskGroupInstanceDAO(connection);
            TaskInstanceDAO taskDAO = new TaskInstanceDAO(connection);
            WorkflowInstanceDAO workflowDAO = new WorkflowInstanceDAO(connection);
            
            // Get all root task groups and top-level tasks
            List<TaskGroupInstance> rootGroups = groupDAO.getRootTaskGroups(workflow.getId());
            List<TaskInstance> topLevelTasks = taskDAO.getTopLevelTasks(workflow.getId());
            
            // Check if all are in a terminal state
            boolean allComplete = true;
            
            for (TaskGroupInstance group : rootGroups) {
                if (!GroupCompletionEvaluator.isTerminalStatus(group.getStatus())) {
                    allComplete = false;
                    break;
                }
            }
            
            if (allComplete) {
                for (TaskInstance task : topLevelTasks) {
                    if (!GroupCompletionEvaluator.isTerminalStatus(task.getStatus())) {
                        allComplete = false;
                        break;
                    }
                }
            }
            
            // If all are complete, check if any failed
            if (allComplete) {
                boolean anyFailed = false;
                
                for (TaskGroupInstance group : rootGroups) {
                    if (group.getStatus() == TaskStatus.FAILED) {
                        anyFailed = true;
                        break;
                    }
                }
                
                if (!anyFailed) {
                    for (TaskInstance task : topLevelTasks) {
                        if (GroupCompletionEvaluator.isTerminalFailureStatus(task.getStatus())) {
                            anyFailed = true;
                            break;
                        }
                    }
                }
                
                // Update workflow status based on completion results
                if (anyFailed) {
                    workflowDAO.updateWorkflowStatus(workflow.getId(), WorkflowStatus.FAILED);
                    LOGGER.info("Workflow failed: " + workflow.getId());
                } else {
                    workflowDAO.updateWorkflowStatus(workflow.getId(), WorkflowStatus.COMPLETED);
                    LOGGER.info("Workflow completed successfully: " + workflow.getId());
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking workflow completion: " + workflow.getId(), e);
        }
    }
}