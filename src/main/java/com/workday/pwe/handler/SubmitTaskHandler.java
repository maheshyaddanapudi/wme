package com.workday.pwe.handler;

import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.enums.TaskType;
import com.workday.pwe.model.TaskInstance;

import java.sql.Connection;
import java.util.logging.Logger;

/**
 * Handler for Submit tasks.
 */
public class SubmitTaskHandler extends TaskHandler {

    private static final Logger LOGGER = Logger.getLogger(SubmitTaskHandler.class.getName());

    /**
     * Constructor with task instance
     * 
     * @param taskInstance The task instance to handle
     */
    protected SubmitTaskHandler(TaskInstance taskInstance) {
        super(taskInstance);
    }

    @Override
    protected String getTaskType() {
        return TaskType.SUBMIT.name();
    }

    @Override
    protected boolean isStateful() {
        return false; // Submit tasks are not stateful
    }

    @Override
    protected TaskStatus getCompletionStatus() {
        return TaskStatus.SUBMITTED; // Custom completion status for Submit tasks
    }

    @Override
    protected void execute(Connection connection) {
        LOGGER.info("Executing SubmitTaskHandler for task: " + getTaskInstance().getId());
        
        // A Submit task typically waits for a human to submit something
        // The task will be completed when the human calls the task completion API
        // No additional execution logic is needed here
    }
}