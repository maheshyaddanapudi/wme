package com.workday.pwe.handler;

import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.enums.TaskType;
import com.workday.pwe.model.TaskInstance;

import java.sql.Connection;
import java.util.logging.Logger;

/**
 * Handler for ToDo tasks.
 */
public class ToDoTaskHandler extends TaskHandler {

    private static final Logger LOGGER = Logger.getLogger(ToDoTaskHandler.class.getName());

    /**
     * Constructor with task instance
     * 
     * @param taskInstance The task instance to handle
     */
    protected ToDoTaskHandler(TaskInstance taskInstance) {
        super(taskInstance);
    }

    @Override
    protected String getTaskType() {
        return TaskType.TODO.name();
    }

    @Override
    protected boolean isStateful() {
        return false; // ToDo tasks are not stateful
    }

    @Override
    protected TaskStatus getCompletionStatus() {
        return TaskStatus.COMPLETED;
    }

    @Override
    protected void execute(Connection connection) {
        LOGGER.info("Executing ToDoTaskHandler for task: " + getTaskInstance().getId());
        
        // A ToDo task typically waits for a human to complete it
        // The task will be completed when the human calls the task completion API
        // No additional execution logic is needed here
    }
}