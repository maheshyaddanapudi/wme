package com.workday.pwe.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.workday.pwe.enums.TaskType;

import java.util.UUID;

/**
 * Represents the definition of a task within a workflow definition.
 */
public class TaskDefinition {
    private UUID id;
    private UUID workflowDefId;
    private UUID taskGroupDefId; // nullable, null if not part of a group
    private String name;
    private TaskType taskType;
    private int taskOrder;
    private JsonNode parametersJson;
    
    // Default constructor
    public TaskDefinition() {
    }
    
    // Constructor with required fields
    public TaskDefinition(UUID workflowDefId, String name, TaskType taskType, int taskOrder) {
        this.id = UUID.randomUUID();
        this.workflowDefId = workflowDefId;
        this.name = name;
        this.taskType = taskType;
        this.taskOrder = taskOrder;
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getWorkflowDefId() {
        return workflowDefId;
    }
    
    public void setWorkflowDefId(UUID workflowDefId) {
        this.workflowDefId = workflowDefId;
    }
    
    public UUID getTaskGroupDefId() {
        return taskGroupDefId;
    }
    
    public void setTaskGroupDefId(UUID taskGroupDefId) {
        this.taskGroupDefId = taskGroupDefId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public TaskType getTaskType() {
        return taskType;
    }
    
    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }
    
    public int getTaskOrder() {
        return taskOrder;
    }
    
    public void setTaskOrder(int taskOrder) {
        this.taskOrder = taskOrder;
    }
    
    public JsonNode getParametersJson() {
        return parametersJson;
    }
    
    public void setParametersJson(JsonNode parametersJson) {
        this.parametersJson = parametersJson;
    }
    
    // Check if this task is part of a group
    public boolean isPartOfGroup() {
        return taskGroupDefId != null;
    }
    
    // Check if this is a system task
    public boolean isSystemTask() {
        return taskType == TaskType.HTTP;
    }
    
    // Check if this is a human task
    public boolean isHumanTask() {
        return !isSystemTask();
    }
}