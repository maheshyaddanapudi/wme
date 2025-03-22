package com.workday.pwe.dto;

import com.workday.pwe.enums.WorkflowStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for workflow query parameters.
 */
public class WorkflowQueryDTO {
    private UUID workflowDefId;
    private WorkflowStatus status;
    private String nameContains;
    private LocalDateTime startedAfter;
    private LocalDateTime startedBefore;
    private LocalDateTime completedAfter;
    private LocalDateTime completedBefore;
    private String assignee;
    private int limit = 100;
    private int offset = 0;
    private String sortBy = "created_at";
    private boolean ascending = false;
    
    // Default constructor
    public WorkflowQueryDTO() {
    }
    
    // Builder pattern for fluent interface
    public WorkflowQueryDTO withWorkflowDefId(UUID workflowDefId) {
        this.workflowDefId = workflowDefId;
        return this;
    }
    
    public WorkflowQueryDTO withStatus(WorkflowStatus status) {
        this.status = status;
        return this;
    }
    
    public WorkflowQueryDTO withNameContains(String nameContains) {
        this.nameContains = nameContains;
        return this;
    }
    
    public WorkflowQueryDTO withStartedAfter(LocalDateTime startedAfter) {
        this.startedAfter = startedAfter;
        return this;
    }
    
    public WorkflowQueryDTO withStartedBefore(LocalDateTime startedBefore) {
        this.startedBefore = startedBefore;
        return this;
    }
    
    public WorkflowQueryDTO withCompletedAfter(LocalDateTime completedAfter) {
        this.completedAfter = completedAfter;
        return this;
    }
    
    public WorkflowQueryDTO withCompletedBefore(LocalDateTime completedBefore) {
        this.completedBefore = completedBefore;
        return this;
    }
    
    public WorkflowQueryDTO withAssignee(String assignee) {
        this.assignee = assignee;
        return this;
    }
    
    public WorkflowQueryDTO withLimit(int limit) {
        this.limit = Math.max(1, Math.min(limit, 1000)); // Constrain between 1 and 1000
        return this;
    }
    
    public WorkflowQueryDTO withOffset(int offset) {
        this.offset = Math.max(0, offset);
        return this;
    }
    
    public WorkflowQueryDTO withSortBy(String sortBy) {
        this.sortBy = sortBy;
        return this;
    }
    
    public WorkflowQueryDTO withAscending(boolean ascending) {
        this.ascending = ascending;
        return this;
    }
    
    // Getters
    public UUID getWorkflowDefId() {
        return workflowDefId;
    }
    
    public WorkflowStatus getStatus() {
        return status;
    }
    
    public String getNameContains() {
        return nameContains;
    }
    
    public LocalDateTime getStartedAfter() {
        return startedAfter;
    }
    
    public LocalDateTime getStartedBefore() {
        return startedBefore;
    }
    
    public LocalDateTime getCompletedAfter() {
        return completedAfter;
    }
    
    public LocalDateTime getCompletedBefore() {
        return completedBefore;
    }
    
    public String getAssignee() {
        return assignee;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public int getOffset() {
        return offset;
    }
    
    public String getSortBy() {
        return sortBy;
    }
    
    public boolean isAscending() {
        return ascending;
    }
    
    // Convert to SQL WHERE clause
    public String toSqlWhereClause() {
        StringBuilder sb = new StringBuilder();
        boolean hasCondition = false;
        
        if (workflowDefId != null) {
            sb.append("workflow_def_id = '").append(workflowDefId).append("'");
            hasCondition = true;
        }
        
        if (status != null) {
            if (hasCondition) sb.append(" AND ");
            sb.append("status = '").append(status).append("'");
            hasCondition = true;
        }
        
        if (nameContains != null && !nameContains.isEmpty()) {
            if (hasCondition) sb.append(" AND ");
            sb.append("name LIKE '%").append(nameContains).append("%'");
            hasCondition = true;
        }
        
        if (startedAfter != null) {
            if (hasCondition) sb.append(" AND ");
            sb.append("start_time > '").append(startedAfter).append("'");
            hasCondition = true;
        }
        
        if (startedBefore != null) {
            if (hasCondition) sb.append(" AND ");
            sb.append("start_time < '").append(startedBefore).append("'");
            hasCondition = true;
        }
        
        if (completedAfter != null) {
            if (hasCondition) sb.append(" AND ");
            sb.append("end_time > '").append(completedAfter).append("'");
            hasCondition = true;
        }
        
        if (completedBefore != null) {
            if (hasCondition) sb.append(" AND ");
            sb.append("end_time < '").append(completedBefore).append("'");
            hasCondition = true;
        }
        
        return hasCondition ? "WHERE " + sb.toString() : "";
    }
    
    // Convert to SQL ORDER BY clause
    public String toSqlOrderByClause() {
        return "ORDER BY " + sortBy + (ascending ? " ASC" : " DESC");
    }
    
    // Convert to SQL LIMIT OFFSET clause
    public String toSqlLimitOffsetClause() {
        return "LIMIT " + limit + " OFFSET " + offset;
    }
}