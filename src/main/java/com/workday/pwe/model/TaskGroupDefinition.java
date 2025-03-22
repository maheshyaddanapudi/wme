package com.workday.pwe.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.workday.pwe.enums.CompletionCriteria;
import com.workday.pwe.enums.TaskGroupType;

import java.util.UUID;

/**
 * Represents the definition of a task group within a workflow definition.
 */
public class TaskGroupDefinition {
    private UUID id;
    private UUID workflowDefId;
    private UUID parentGroupDefId; // nullable, null if this is a root group
    private String name;
    private TaskGroupType groupType;
    private CompletionCriteria completionCriteria;
    private int groupOrder;
    private JsonNode parametersJson;
    
    // Default constructor
    public TaskGroupDefinition() {
    }
    
    // Constructor with required fields
    public TaskGroupDefinition(UUID workflowDefId, String name, TaskGroupType groupType, 
                             CompletionCriteria completionCriteria, int groupOrder) {
        this.id = UUID.randomUUID();
        this.workflowDefId = workflowDefId;
        this.name = name;
        this.groupType = groupType;
        this.completionCriteria = completionCriteria;
        this.groupOrder = groupOrder;
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
    
    public UUID getParentGroupDefId() {
        return parentGroupDefId;
    }
    
    public void setParentGroupDefId(UUID parentGroupDefId) {
        this.parentGroupDefId = parentGroupDefId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public TaskGroupType getGroupType() {
        return groupType;
    }
    
    public void setGroupType(TaskGroupType groupType) {
        this.groupType = groupType;
    }
    
    public CompletionCriteria getCompletionCriteria() {
        return completionCriteria;
    }
    
    public void setCompletionCriteria(CompletionCriteria completionCriteria) {
        this.completionCriteria = completionCriteria;
    }
    
    public int getGroupOrder() {
        return groupOrder;
    }
    
    public void setGroupOrder(int groupOrder) {
        this.groupOrder = groupOrder;
    }
    
    public JsonNode getParametersJson() {
        return parametersJson;
    }
    
    public void setParametersJson(JsonNode parametersJson) {
        this.parametersJson = parametersJson;
    }
    
    // Check if this is a root group
    public boolean isRootGroup() {
        return parentGroupDefId == null;
    }
    
    // Check if this is a vertical group
    public boolean isVertical() {
        return groupType == TaskGroupType.VERTICAL;
    }
    
    // Check if this is a horizontal group
    public boolean isHorizontal() {
        return groupType == TaskGroupType.HORIZONTAL;
    }
}