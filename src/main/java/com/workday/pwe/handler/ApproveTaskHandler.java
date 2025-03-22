package com.workday.pwe.handler;

import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.enums.TaskType;
import com.workday.pwe.model.TaskInstance;

import java.sql.Connection;
import java.util.logging.Logger;

/**
 * Handler for Approve tasks.
 */
public class ApproveTaskHandler extends TaskHandler {

    private static final Logger LOGGER = Logger.getLogger(ApproveTaskHandler.class.getName());

    /**
     * Constructor with task instance
     * 
     * @param taskInstance The task instance to handle
     */
    protected ApproveTaskHandler(TaskInstance taskInstance) {
        super(taskInstance);
    }

    @Override
    protected String getTaskType() {
        return TaskType.APPROVE.name();
    }

    @Override
    protected boolean isStateful() {
        return false; // Approve tasks are not stateful
    }

    @Override
    protected TaskStatus getCompletionStatus() {
        return TaskStatus.APPROVED; // Custom completion status for Approve tasks
    }

    @Override
    protected void execute(Connection connection) {
        LOGGER.info("Executing ApproveTaskHandler for task: " + getTaskInstance().getId());
        
        // An Approve task typically waits for a human to approve something
        // The task will be completed when the human calls the task completion API
        // No additional execution logic is needed here
    }
}