package com.workday.pwe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.workday.pwe.dao.WorkflowHistoryDAO;
import com.workday.pwe.model.WorkflowHistory;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for workflow history and audit operations.
 */
public class HistoryAndAuditService {

    private static final Logger LOGGER = Logger.getLogger(HistoryAndAuditService.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Records a status change event
     * 
     * @param connection Database connection
     * @param workflowInstanceId Workflow instance ID
     * @param entityType Entity type (WORKFLOW, TASK, TASK_GROUP)
     * @param entityId Entity ID
     * @param oldStatus Old status
     * @param newStatus New status
     * @param username User who made the change
     * @return The created history record
     */
    public WorkflowHistory recordStatusChange(Connection connection, UUID workflowInstanceId, 
                                             String entityType, UUID entityId, 
                                             String oldStatus, String newStatus, 
                                             String username) {
        try {
            WorkflowHistoryDAO historyDAO = new WorkflowHistoryDAO(connection);
            
            // Create details JSON
            ObjectNode detailsJson = OBJECT_MAPPER.createObjectNode();
            detailsJson.put("oldStatus", oldStatus);
            detailsJson.put("newStatus", newStatus);
            
            // Create history record
            WorkflowHistory history = new WorkflowHistory(workflowInstanceId, entityType, entityId, "STATUS_CHANGE", detailsJson);
            history.setUsername(username);
            
            UUID historyId = historyDAO.addHistoryRecord(history);
            history.setId(historyId);
            
            return history;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error recording status change", e);
            throw new RuntimeException("Error recording status change", e);
        }
    }
    
    /**
     * Records an assignment change event
     * 
     * @param connection Database connection
     * @param workflowInstanceId Workflow instance ID
     * @param taskId Task ID
     * @param oldAssignee Old assignee
     * @param newAssignee New assignee
     * @param username User who made the change
     * @return The created history record
     */
    public WorkflowHistory recordAssignmentChange(Connection connection, UUID workflowInstanceId, 
                                                UUID taskId, String oldAssignee, 
                                                String newAssignee, String username) {
        try {
            WorkflowHistoryDAO historyDAO = new WorkflowHistoryDAO(connection);
            
            // Create details JSON
            ObjectNode detailsJson = OBJECT_MAPPER.createObjectNode();
            detailsJson.put("oldAssignee", oldAssignee);
            detailsJson.put("newAssignee", newAssignee);
            
            // Create history record
            WorkflowHistory history = new WorkflowHistory(workflowInstanceId, "TASK", taskId, "ASSIGNMENT_CHANGE", detailsJson);
            history.setUsername(username);
            
            UUID historyId = historyDAO.addHistoryRecord(history);
            history.setId(historyId);
            
            return history;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error recording assignment change", e);
            throw new RuntimeException("Error recording assignment change", e);
        }
    }
    
    /**
     * Gets history for a workflow
     * 
     * @param connection Database connection
     * @param workflowInstanceId Workflow instance ID
     * @return List of history records
     */
    public List<WorkflowHistory> getHistoryForWorkflow(Connection connection, UUID workflowInstanceId) {
        try {
            WorkflowHistoryDAO historyDAO = new WorkflowHistoryDAO(connection);
            return historyDAO.getHistoryForWorkflow(workflowInstanceId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting workflow history", e);
            throw new RuntimeException("Error getting workflow history", e);
        }
    }
    
    /**
     * Gets history for an entity
     * 
     * @param connection Database connection
     * @param entityType Entity type
     * @param entityId Entity ID
     * @return List of history records
     */
    public List<WorkflowHistory> getHistoryForEntity(Connection connection, String entityType, UUID entityId) {
        try {
            WorkflowHistoryDAO historyDAO = new WorkflowHistoryDAO(connection);
            return historyDAO.getHistoryForEntity(entityType, entityId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting entity history", e);
            throw new RuntimeException("Error getting entity history", e);
        }
    }
    
    /**
     * Archives old history records
     * 
     * @param connection Database connection
     * @param olderThan Records older than this date will be archived
     * @return Number of records archived
     */
    public int archiveOldHistory(Connection connection, LocalDateTime olderThan) {
        try {
            WorkflowHistoryDAO historyDAO = new WorkflowHistoryDAO(connection);
            return historyDAO.archiveOldHistory(olderThan);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error archiving old history", e);
            throw new RuntimeException("Error archiving old history", e);
        }
    }
}