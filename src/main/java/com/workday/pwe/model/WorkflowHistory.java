package com.workday.pwe.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a historical record of changes to workflow entities.
 */
public class WorkflowHistory {
    private UUID id;
    private UUID workflowInstanceId;
    private String entityType;  // "WORKFLOW", "TASK_GROUP", "TASK"
    private UUID entityId;      // ID of the entity that changed
    private String changeType;  // "STATUS_CHANGE", "ASSIGNMENT_CHANGE", "PARAMETER_CHANGE", etc.
    private JsonNode detailsJson; // Change details in JSON format
    private LocalDateTime timestamp;
    private String username;    // User who made the change (if applicable)
    
    // Default constructor
    public WorkflowHistory() {
    }
    
    // Constructor with required fields
    public WorkflowHistory(UUID workflowInstanceId, String entityType, UUID entityId, 
                         String changeType, JsonNode detailsJson) {
        this.id = UUID.randomUUID();
        this.workflowInstanceId = workflowInstanceId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.changeType = changeType;
        this.detailsJson = detailsJson;
        this.timestamp = LocalDateTime.now();
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
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public UUID getEntityId() {
        return entityId;
    }
    
    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }
    
    public String getChangeType() {
        return changeType;
    }
    
    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }
    
    public JsonNode getDetailsJson() {
        return detailsJson;
    }
    
    public void setDetailsJson(JsonNode detailsJson) {
        this.detailsJson = detailsJson;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    // Factory methods for common history records
    
    // Create a status change record for a workflow
    public static WorkflowHistory createWorkflowStatusChange(UUID workflowInstanceId, 
                                                          String oldStatus, 
                                                          String newStatus) {
        // For simplicity, we're manually creating the JSON for this example
        // In a real implementation, you would use a proper JSON library
        String details = "{\"oldStatus\":\"" + oldStatus + "\",\"newStatus\":\"" + newStatus + "\"}";
        JsonNode detailsJson = null; // In real implementation, parse the JSON string
        return new WorkflowHistory(workflowInstanceId, "WORKFLOW", workflowInstanceId, 
                                "STATUS_CHANGE", detailsJson);
    }
    
    // Create a status change record for a task
    public static WorkflowHistory createTaskStatusChange(UUID workflowInstanceId, 
                                                      UUID taskInstanceId,
                                                      String oldStatus, 
                                                      String newStatus) {
        // For simplicity, we're manually creating the JSON for this example
        String details = "{\"oldStatus\":\"" + oldStatus + "\",\"newStatus\":\"" + newStatus + "\"}";
        JsonNode detailsJson = null; // In real implementation, parse the JSON string
        return new WorkflowHistory(workflowInstanceId, "TASK", taskInstanceId, 
                                "STATUS_CHANGE", detailsJson);
    }
    
    // Create a status change record for a task group
    public static WorkflowHistory createTaskGroupStatusChange(UUID workflowInstanceId, 
                                                           UUID taskGroupInstanceId,
                                                           String oldStatus, 
                                                           String newStatus) {
        // For simplicity, we're manually creating the JSON for this example
        String details = "{\"oldStatus\":\"" + oldStatus + "\",\"newStatus\":\"" + newStatus + "\"}";
        JsonNode detailsJson = null; // In real implementation, parse the JSON string
        return new WorkflowHistory(workflowInstanceId, "TASK_GROUP", taskGroupInstanceId, 
                                "STATUS_CHANGE", detailsJson);
    }
    
    // Create an assignment change record for a task
    public static WorkflowHistory createTaskAssignmentChange(UUID workflowInstanceId, 
                                                          UUID taskInstanceId,
                                                          String oldAssignee, 
                                                          String newAssignee) {
        // For simplicity, we're manually creating the JSON for this example
        String details = "{\"oldAssignee\":\"" + oldAssignee + "\",\"newAssignee\":\"" + newAssignee + "\"}";
        JsonNode detailsJson = null; // In real implementation, parse the JSON string
        return new WorkflowHistory(workflowInstanceId, "TASK", taskInstanceId, 
                                "ASSIGNMENT_CHANGE", detailsJson);
    }
}