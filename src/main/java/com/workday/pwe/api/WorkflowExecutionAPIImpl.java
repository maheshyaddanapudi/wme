package com.workday.pwe.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.workday.pwe.api.WorkflowExecutionAPI;
import com.workday.pwe.model.WorkflowHistory;
import com.workday.pwe.model.WorkflowInstance;
import com.workday.pwe.service.HistoryAndAuditService;
import com.workday.pwe.service.WorkflowControlService;
import com.workday.pwe.service.WorkflowInstanceService;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the WorkflowExecutionAPI interface.
 */
public class WorkflowExecutionAPIImpl implements WorkflowExecutionAPI {

    private static final Logger LOGGER = Logger.getLogger(WorkflowExecutionAPIImpl.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final Connection connection;
    private final WorkflowInstanceService workflowInstanceService;
    private final WorkflowControlService workflowControlService;
    private final HistoryAndAuditService historyService;
    
    /**
     * Constructor with dependencies
     * 
     * @param connection Database connection
     */
    public WorkflowExecutionAPIImpl(Connection connection) {
        this.connection = connection;
        this.workflowInstanceService = new WorkflowInstanceService();
        this.workflowControlService = new WorkflowControlService();
        this.historyService = new HistoryAndAuditService();
    }

    @Override
    public WorkflowInstance startWorkflow(UUID workflowDefId, JsonNode inputJson) {
        try {
            return workflowInstanceService.startWorkflow(connection, workflowDefId, inputJson);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error starting workflow", e);
            throw new RuntimeException("Error starting workflow", e);
        }
    }

    @Override
    public WorkflowInstance getWorkflow(UUID id) {
        try {
            return workflowInstanceService.getWorkflowInstance(connection, id);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting workflow instance", e);
            throw new RuntimeException("Error getting workflow instance", e);
        }
    }

    @Override
    public List<WorkflowInstance> getWorkflowsByDefinition(UUID workflowDefId) {
        try {
            return workflowInstanceService.getWorkflowInstancesByDefinitionId(connection, workflowDefId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting workflow instances by definition", e);
            throw new RuntimeException("Error getting workflow instances by definition", e);
        }
    }

    @Override
    public boolean pauseWorkflow(UUID id) {
        try {
            return workflowControlService.pauseWorkflow(connection, id);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error pausing workflow", e);
            throw new RuntimeException("Error pausing workflow", e);
        }
    }

    @Override
    public boolean resumeWorkflow(UUID id) {
        try {
            return workflowControlService.resumeWorkflow(connection, id);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error resuming workflow", e);
            throw new RuntimeException("Error resuming workflow", e);
        }
    }

    @Override
    public boolean terminateWorkflow(UUID id, String reason) {
        try {
            return workflowControlService.terminateWorkflow(connection, id, reason);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error terminating workflow", e);
            throw new RuntimeException("Error terminating workflow", e);
        }
    }

    @Override
    public JsonNode getWorkflowHistory(UUID id) {
        try {
            List<WorkflowHistory> historyRecords = historyService.getHistoryForWorkflow(connection, id);
            
            // Convert history records to JSON
            ArrayNode historyJson = OBJECT_MAPPER.createArrayNode();
            for (WorkflowHistory record : historyRecords) {
                historyJson.add(OBJECT_MAPPER.valueToTree(record));
            }
            
            return historyJson;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting workflow history", e);
            throw new RuntimeException("Error getting workflow history", e);
        }
    }
}