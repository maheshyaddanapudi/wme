package com.workday.pwe.handler;

import com.workday.pwe.dao.TaskDefinitionDAO;
import com.workday.pwe.enums.TaskType;
import com.workday.pwe.model.TaskDefinition;
import com.workday.pwe.model.TaskInstance;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry for task handlers that maps task types to their handlers.
 */
public class TaskHandlerRegistry {

    private static final Logger LOGGER = Logger.getLogger(TaskHandlerRegistry.class.getName());
    
    private static final Map<String, Class<? extends TaskHandler>> HANDLER_MAP = new ConcurrentHashMap<>();
    
    // Initialize the registry with default handlers
    static {
        registerHandler(TaskType.TODO.name(), ToDoTaskHandler.class);
        registerHandler(TaskType.SUBMIT.name(), SubmitTaskHandler.class);
        registerHandler(TaskType.APPROVE.name(), ApproveTaskHandler.class);
        registerHandler(TaskType.REVIEW.name(), ReviewTaskHandler.class);
        registerHandler(TaskType.HTTP.name(), HttpTaskHandler.class);
    }
    
    /**
     * Private constructor to prevent instantiation
     */
    private TaskHandlerRegistry() {
        // Do not instantiate
    }
    
    /**
     * Register a task handler for a specific task type
     * 
     * @param taskType The task type
     * @param handlerClass The handler class for the task type
     */
    public static void registerHandler(String taskType, Class<? extends TaskHandler> handlerClass) {
        HANDLER_MAP.put(taskType, handlerClass);
        LOGGER.info("Registered handler " + handlerClass.getName() + " for task type " + taskType);
    }
    
    /**
     * Get the appropriate task handler for a task instance
     * 
     * @param connection Database connection
     * @param taskInstance The task instance
     * @return The appropriate task handler
     */
    public static TaskHandler getHandler(Connection connection, TaskInstance taskInstance) {
        try {
            // Get the task definition to determine the task type
            TaskDefinitionDAO taskDefDAO = new TaskDefinitionDAO(connection);
            TaskDefinition taskDef = taskDefDAO.getTaskDefinition(taskInstance.getTaskDefId());
            
            if (taskDef == null) {
                throw new IllegalArgumentException("Task definition not found for task instance: " + taskInstance.getId());
            }
            
            String taskType = taskDef.getTaskType().name();
            
            // Get the handler class for the task type
            Class<? extends TaskHandler> handlerClass = HANDLER_MAP.get(taskType);
            
            if (handlerClass == null) {
                throw new IllegalArgumentException("No handler registered for task type: " + taskType);
            }
            
            // Create an instance of the handler using reflection
            Constructor<? extends TaskHandler> constructor = handlerClass.getDeclaredConstructor(TaskInstance.class);
            constructor.setAccessible(true); // Make constructor accessible even if protected
            TaskHandler handler = constructor.newInstance(taskInstance);
            
            // Set connection for stateful handlers
            if (handler.isStateful()) {
                handler.setConnection(connection);
            }
            
            return handler;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating task handler", e);
            throw new RuntimeException("Error creating task handler for task instance: " + taskInstance.getId(), e);
        }
    }
    
    /**
     * Get all registered task types and their handlers
     * 
     * @return Map of task types to handler class names
     */
    public static Map<String, String> getRegisteredHandlers() {
        Map<String, String> result = new HashMap<>();
        HANDLER_MAP.forEach((type, clazz) -> result.put(type, clazz.getName()));
        return result;
    }
}