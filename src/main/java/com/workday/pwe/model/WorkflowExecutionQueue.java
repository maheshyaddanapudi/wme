package com.workday.pwe.model;

import com.workday.pwe.enums.QueueStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an entry in the workflow execution queue for state management.
 */
public class WorkflowExecutionQueue {
    private UUID id;
    private UUID workflowInstanceId;
    private QueueStatus status;
    private int priority;
    private LocalDateTime lastUpdated;
    private LocalDateTime createdAt;
    
    // Default constructor
    public WorkflowExecutionQueue() {
    }
    
    // Constructor with required fields
    public WorkflowExecutionQueue(UUID workflowInstanceId) {
        this.id = UUID.randomUUID();
        this.workflowInstanceId = workflowInstanceId;
        this.status = QueueStatus.PENDING;
        this.priority = 0; // Default priority
        this.lastUpdated = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }
    
    // Constructor with priority
    public WorkflowExecutionQueue(UUID workflowInstanceId, int priority) {
        this.id = UUID.randomUUID();
        this.workflowInstanceId = workflowInstanceId;
        this.status = QueueStatus.PENDING;
        this.priority = priority;
        this.lastUpdated = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
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
    
    public QueueStatus getStatus() {
        return status;
    }
    
    public void setStatus(QueueStatus status) {
        this.status = status;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    // Mark the entry as processing
    public void markProcessing() {
        this.status = QueueStatus.PROCESSING;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Mark the entry as completed
    public void markCompleted() {
        this.status = QueueStatus.COMPLETED;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Mark the entry as failed
    public void markFailed() {
        this.status = QueueStatus.FAILED;
        this.lastUpdated = LocalDateTime.now();
    }
}