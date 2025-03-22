package com.workday.pwe.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.workday.pwe.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a runtime instance of a task within a workflow instance.
 */
public class TaskInstance {
    private UUID id;
    private UUID workflowInstanceId;
    private UUID taskDefId;
    private UUID taskGroupInstanceId; // nullable, null if not part of a group
    private String assignee;
    private TaskStatus status;
    private JsonNode inputJson;
    private JsonNode outputJson;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime dueDate; // optional, for tasks with deadline
    private String failureReason; // optional, populated if task fails
    
    // Default constructor
    public TaskInstance() {
    }
    
    // Constructor with required fields
    public TaskInstance(UUID workflowInstanceId, UUID taskDefId, String assignee) {
        this.id = UUID.randomUUID();
        this.workflowInstanceId = workflowInstanceId;
        this.taskDefId = taskDefId;
        this.assignee = assignee;
        this.status = TaskStatus.NOT_STARTED;
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
    
    public UUID getTaskDefId() {
        return taskDefId;
    }
    
    public void setTaskDefId(UUID taskDefId) {
        this.taskDefId = taskDefId;
    }
    
    public UUID getTaskGroupInstanceId() {
        return taskGroupInstanceId;
    }
    
    public void setTaskGroupInstanceId(UUID taskGroupInstanceId) {
        this.taskGroupInstanceId = taskGroupInstanceId;
    }
    
    public String getAssignee() {
        return assignee;
    }
    
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
        
        // Set timestamps based on status changes
        if (status == TaskStatus.IN_PROGRESS && this.startTime == null) {
            this.startTime = LocalDateTime.now();
        } else if (isTerminalStatus(status) && this.endTime == null) {
            this.endTime = LocalDateTime.now();
        }
    }
    
    public JsonNode getInputJson() {
        return inputJson;
    }
    
    public void setInputJson(JsonNode inputJson) {
        this.inputJson = inputJson;
    }
    
    public JsonNode getOutputJson() {
        return outputJson;
    }
    
    public void setOutputJson(JsonNode outputJson) {
        this.outputJson = outputJson;
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
    
    public LocalDateTime getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    // Check if part of a group
    public boolean isPartOfGroup() {
        return taskGroupInstanceId != null;
    }
    
    // Check if the task is completed
    public boolean isCompleted() {
        return isTerminalStatus(this.status);
    }
    
    // Check if the status is terminal
    private boolean isTerminalStatus(TaskStatus status) {
        return status == TaskStatus.COMPLETED || 
               status == TaskStatus.SUBMITTED || 
               status == TaskStatus.APPROVED || 
               status == TaskStatus.REVIEWED || 
               status == TaskStatus.API_CALL_COMPLETE || 
               status == TaskStatus.FAILED || 
               status == TaskStatus.EXPIRED || 
               status == TaskStatus.SKIPPED;
    }
    
    // Check if the task is past due
    public boolean isPastDue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate);
    }
    
    // Start the task
    public void start() {
        if (this.status == TaskStatus.NOT_STARTED) {
            this.status = TaskStatus.IN_PROGRESS;
            this.startTime = LocalDateTime.now();
        }
    }
    
    // Complete the task with output data
    public void complete(JsonNode output) {
        if (this.status == TaskStatus.IN_PROGRESS) {
            this.status = TaskStatus.COMPLETED;
            this.outputJson = output;
            this.endTime = LocalDateTime.now();
        }
    }
    
    // Submit the task with output data
    public void submit(JsonNode output) {
        if (this.status == TaskStatus.IN_PROGRESS) {
            this.status = TaskStatus.SUBMITTED;
            this.outputJson = output;
            this.endTime = LocalDateTime.now();
        }
    }
    
    // Approve the task with output data
    public void approve(JsonNode output) {
        if (this.status == TaskStatus.IN_PROGRESS) {
            this.status = TaskStatus.APPROVED;
            this.outputJson = output;
            this.endTime = LocalDateTime.now();
        }
    }
    
    // Review the task with output data
    public void review(JsonNode output) {
        if (this.status == TaskStatus.IN_PROGRESS) {
            this.status = TaskStatus.REVIEWED;
            this.outputJson = output;
            this.endTime = LocalDateTime.now();
        }
    }
    
    // Complete API call with output data
    public void completeApiCall(JsonNode output) {
        if (this.status == TaskStatus.IN_PROGRESS) {
            this.status = TaskStatus.API_CALL_COMPLETE;
            this.outputJson = output;
            this.endTime = LocalDateTime.now();
        }
    }
    
    // Fail the task with a reason
    public void fail(String reason) {
        if (this.status == TaskStatus.IN_PROGRESS || this.status == TaskStatus.NOT_STARTED) {
            this.status = TaskStatus.FAILED;
            this.failureReason = reason;
            this.endTime = LocalDateTime.now();
        }
    }
    
    // Expire the task
    public void expire() {
        if (this.status == TaskStatus.IN_PROGRESS || this.status == TaskStatus.NOT_STARTED) {
            this.status = TaskStatus.EXPIRED;
            this.endTime = LocalDateTime.now();
        }
    }
    
    // Skip the task
    public void skip() {
        this.status = TaskStatus.SKIPPED;
        this.endTime = LocalDateTime.now();
    }
}