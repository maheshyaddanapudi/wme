package com.workday.pwe.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.workday.pwe.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a runtime instance of a task group within a workflow instance.
 */
public class TaskGroupInstance {
    private UUID id;
    private UUID workflowInstanceId;
    private UUID taskGroupDefId;
    private UUID parentGroupInstId; // nullable, null if this is a root group
    private TaskStatus status;
    private int minCompletion; // Used for N_OF_M completion criteria
    private JsonNode parametersJson;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    // Default constructor
    public TaskGroupInstance() {
    }
    
    // Constructor with required fields
    public TaskGroupInstance(UUID workflowInstanceId, UUID taskGroupDefId, TaskStatus status) {
        this.id = UUID.randomUUID();
        this.workflowInstanceId = workflowInstanceId;
        this.taskGroupDefId = taskGroupDefId;
        this.status = status;
        this.startTime = LocalDateTime.now();
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getWorkflowInstanceId() {
        return workflowInstanceId;
    }
    
    public void setWorkflowInstanceId(UUID workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }
    
    public UUID getTaskGroupDefId() {
        return taskGroupDefId;
    }
    
    public void setTaskGroupDefId(UUID taskGroupDefId) {
        this.taskGroupDefId = taskGroupDefId;
    }
    
    public UUID getParentGroupInstId() {
        return parentGroupInstId;
    }
    
    public void setParentGroupInstId(UUID parentGroupInstId) {
        this.parentGroupInstId = parentGroupInstId;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
        
        // Set end time if the status is terminal
        if (isTerminalStatus(status) && this.endTime == null) {
            this.endTime = LocalDateTime.now();
        }
    }
    
    public int getMinCompletion() {
        return minCompletion;
    }
    
    public void setMinCompletion(int minCompletion) {
        this.minCompletion = minCompletion;
    }
    
    public JsonNode getParametersJson() {
        return parametersJson;
    }
    
    public void setParametersJson(JsonNode parametersJson) {
        this.parametersJson = parametersJson;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    // Check if this is a root group
    public boolean isRootGroup() {
        return parentGroupInstId == null;
    }
    
    // Check if the group is completed
    public boolean isCompleted() {
        return this.status == TaskStatus.COMPLETED;
    }
    
    // Check if the status is terminal
    private boolean isTerminalStatus(TaskStatus status) {
        return status == TaskStatus.COMPLETED || 
               status == TaskStatus.FAILED || 
               status == TaskStatus.SKIPPED;
    }
    
    // Start the task group
    public void start() {
        if (this.status == TaskStatus.NOT_STARTED) {
            this.status = TaskStatus.IN_PROGRESS;
            this.startTime = LocalDateTime.now();
        }
    }
    
    // Complete the task group
    public void complete() {
        if (this.status == TaskStatus.IN_PROGRESS || this.status == TaskStatus.BLOCKED) {
            this.status = TaskStatus.COMPLETED;
            this.endTime = LocalDateTime.now();
        }
    }
    
    // Fail the task group
    public void fail() {
        if (this.status == TaskStatus.IN_PROGRESS || this.status == TaskStatus.BLOCKED) {
            this.status = TaskStatus.FAILED;
            this.endTime = LocalDateTime.now();
        }
    }
    
    // Skip the task group
    public void skip() {
        this.status = TaskStatus.SKIPPED;
        this.endTime = LocalDateTime.now();
    }
}