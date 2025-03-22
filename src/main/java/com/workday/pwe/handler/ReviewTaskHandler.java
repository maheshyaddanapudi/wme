package com.workday.pwe.handler;

import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.enums.TaskType;
import com.workday.pwe.model.TaskInstance;

import java.sql.Connection;
import java.util.logging.Logger;

/**
 * Handler for Review tasks.
 */
public class ReviewTaskHandler extends TaskHandler {

    private static final Logger LOGGER = Logger.getLogger(ReviewTaskHandler.class.getName());

    /**
     * Constructor with task instance
     * 
     * @param taskInstance The task instance to handle
     */
    protected ReviewTaskHandler(TaskInstance taskInstance) {
        super(taskInstance);
    }

    @Override
    protected String getTaskType() {
        return TaskType.REVIEW.name();
    }

    @Override
    protected boolean isStateful() {
        return false; // Review tasks are not stateful
    }

    @Override
    protected TaskStatus getCompletionStatus() {
        return TaskStatus.REVIEWED; // Custom completion status for Review tasks
    }

    @Override
    protected void execute(Connection connection) {
        LOGGER.info("Executing ReviewTaskHandler for task: " + getTaskInstance().getId());
        
        // A Review task typically waits for a human to review something
        // The task will be completed when the human calls the task completion API
        // No additional execution logic is needed here
    }
}