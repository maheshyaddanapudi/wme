package com.workday.pwe.api;

import com.workday.pwe.api.QueryAndReportAPI;
import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.enums.WorkflowStatus;
import com.workday.pwe.model.TaskInstance;
import com.workday.pwe.model.WorkflowHistory;
import com.workday.pwe.model.WorkflowInstance;
import com.workday.pwe.service.HistoryAndAuditService;
import com.workday.pwe.service.QueryService;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the QueryAndReportAPI interface.
 */
public class QueryAndReportAPIImpl implements QueryAndReportAPI {

    private static final Logger LOGGER = Logger.getLogger(QueryAndReportAPIImpl.class.getName());
    
    private final Connection connection;
    private final QueryService queryService;
    private final HistoryAndAuditService historyService;
    
    /**
     * Constructor with dependencies
     * 
     * @param connection Database connection
     */
    public QueryAndReportAPIImpl(Connection connection) {
        this.connection = connection;
        this.queryService = new QueryService();
        this.historyService = new HistoryAndAuditService();
    }

    @Override
    public List<WorkflowInstance> queryWorkflows(WorkflowStatus status) {
        try {
            return queryService.findWorkflowsByStatus(connection, status);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error querying workflows", e);
            throw new RuntimeException("Error querying workflows", e);
        }
    }

    @Override
    public List<TaskInstance> queryTasks(TaskStatus status) {
        try {
            return queryService.findTasksByStatus(connection, status);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error querying tasks", e);
            throw new RuntimeException("Error querying tasks", e);
        }
    }

    @Override
    public List<TaskInstance> queryTasksByAssignee(String assignee) {
        try {
            return queryService.findTasksByAssignee(connection, assignee);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error querying tasks by assignee", e);
            throw new RuntimeException("Error querying tasks by assignee", e);
        }
    }

    @Override
    public List<TaskInstance> queryOverdueTasks() {
        try {
            return queryService.findOverdueTasks(connection);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error querying overdue tasks", e);
            throw new RuntimeException("Error querying overdue tasks", e);
        }
    }

    @Override
    public List<WorkflowHistory> getAuditHistory(UUID workflowInstanceId) {
        try {
            return historyService.getHistoryForWorkflow(connection, workflowInstanceId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting audit history", e);
            throw new RuntimeException("Error getting audit history", e);
        }
    }

    @Override
    public List<WorkflowHistory> getEntityHistory(String entityType, UUID entityId) {
        try {
            return historyService.getHistoryForEntity(connection, entityType, entityId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting entity history", e);
            throw new RuntimeException("Error getting entity history", e);
        }
    }
}