package com.workday.pwe.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.workday.pwe.enums.WorkflowStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a running instance of a workflow definition.
 */
public class WorkflowInstance {
    private UUID id;
    private UUID workflowDefId;
    private WorkflowStatus status;
    private JsonNode inputJson;
    private JsonNode outputJson;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Default constructor
    public WorkflowInstance() {
    }
    
    // Constructor with required fields
    public WorkflowInstance(UUID workflowDefId, JsonNode inputJson) {
        this.id = UUID.randomUUID();
        this.workflowDefId = workflowDefId;
        this.status = WorkflowStatus.NOT_STARTED;
        this.inputJson = inputJson;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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
    
    public WorkflowStatus getStatus() {
        return status;
    }
    
    public void setStatus(WorkflowStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Start the workflow
    public void start() {
        if (this.status == WorkflowStatus.NOT_STARTED) {
            this.status = WorkflowStatus.RUNNING;
            this.startTime = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    // Complete the workflow
    public void complete(JsonNode output) {
        if (this.status == WorkflowStatus.RUNNING) {
            this.status = WorkflowStatus.COMPLETED;
            this.outputJson = output;
            this.endTime = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    // Fail the workflow
    public void fail() {
        if (this.status == WorkflowStatus.RUNNING) {
            this.status = WorkflowStatus.FAILED;
            this.endTime = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    // Pause the workflow
    public void pause() {
        if (this.status == WorkflowStatus.RUNNING) {
            this.status = WorkflowStatus.PAUSED;
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    // Resume the workflow
    public void resume() {
        if (this.status == WorkflowStatus.PAUSED) {
            this.status = WorkflowStatus.RUNNING;
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    // Terminate the workflow
    public void terminate() {
        if (this.status == WorkflowStatus.RUNNING || this.status == WorkflowStatus.PAUSED) {
            this.status = WorkflowStatus.TERMINATED;
            this.endTime = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
        }
    }
}