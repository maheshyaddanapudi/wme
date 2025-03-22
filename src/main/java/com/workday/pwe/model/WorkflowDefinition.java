package com.workday.pwe.model;

import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents the definition of a workflow. 
 * This serves as a template for creating workflow instances.
 */
public class WorkflowDefinition {
    private UUID id;
    private String name;
    private int version;
    private JsonNode definitionJson;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Default constructor
    public WorkflowDefinition() {
    }
    
    // Constructor with required fields
    public WorkflowDefinition(String name, JsonNode definitionJson) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.version = 1;
        this.definitionJson = definitionJson;
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    public JsonNode getDefinitionJson() {
        return definitionJson;
    }
    
    public void setDefinitionJson(JsonNode definitionJson) {
        this.definitionJson = definitionJson;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
    
    // Create a new version based on this definition
    public WorkflowDefinition createNewVersion(JsonNode newDefinitionJson) {
        WorkflowDefinition newVersion = new WorkflowDefinition();
        newVersion.setId(UUID.randomUUID());
        newVersion.setName(this.name);
        newVersion.setVersion(this.version + 1);
        newVersion.setDefinitionJson(newDefinitionJson);
        newVersion.setDescription(this.description);
        newVersion.setCreatedAt(LocalDateTime.now());
        newVersion.setUpdatedAt(LocalDateTime.now());
        return newVersion;
    }
}