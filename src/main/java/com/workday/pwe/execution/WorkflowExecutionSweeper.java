package com.workday.pwe.execution;

import java.sql.Connection;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.workday.pwe.dao.WorkflowExecutionQueueDAO;
import com.workday.pwe.util.TenantInfoHolder;

/**
 * Periodically sweeps pending workflows for execution per tenant.
 * Uses virtual threads for efficient execution (requires JDK 21).
 */
public class WorkflowExecutionSweeper {

    private static final Logger LOGGER = Logger.getLogger(WorkflowExecutionSweeper.class.getName());

    private static final int MAX_VIRTUAL_THREADS;
    private static final int SWEEP_INTERVAL_MS;
    private static boolean isFirstSweep = true;
    private static Timestamp lastPollTime = null;
    
    // Load configuration properties
    static {
        Properties properties = new Properties();
        // In a real implementation, we would load from a properties file
        // For now, we'll use default values
        MAX_VIRTUAL_THREADS = Integer.parseInt(properties.getProperty("max.virtual.threads", "50"));
        SWEEP_INTERVAL_MS = Integer.parseInt(properties.getProperty("sweep.interval.ms", "10000"));
    }

    private final ExecutorService virtualThreadPool = Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore threadLimitSemaphore = new Semaphore(MAX_VIRTUAL_THREADS);
    private final APMultiTenantDatasource dataSource;
    private final ScheduledExecutorService scheduler;
    private boolean isRunning = false;

    /**
     * Private constructor for singleton pattern
     */
    private WorkflowExecutionSweeper() {
        this.dataSource = new APMultiTenantDatasource();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "workflow-sweeper-scheduler");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Singleton instance holder
     */
    private static class InstanceHolder {
        static final WorkflowExecutionSweeper INSTANCE = new WorkflowExecutionSweeper();
    }
    
    /**
     * Get the singleton instance
     * 
     * @return The singleton instance
     */
    public static WorkflowExecutionSweeper getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Initialize and start the sweeper
     */
    public void init() {
        if (isRunning) {
            LOGGER.warning("WorkflowExecutionSweeper is already running");
            return;
        }
        
        LOGGER.info("Starting WorkflowExecutionSweeper with max virtual threads: " + MAX_VIRTUAL_THREADS);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sweepWorkflows();
                isFirstSweep = false;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in WorkflowExecutionSweeper loop", e);
            }
        }, 0, SWEEP_INTERVAL_MS, TimeUnit.MILLISECONDS);
        
        isRunning = true;
    }
    
    /**
     * Shutdown the sweeper
     */
    public void shutdown() {
        if (!isRunning) {
            LOGGER.warning("WorkflowExecutionSweeper is not running");
            return;
        }
        
        LOGGER.info("Shutting down WorkflowExecutionSweeper");
        
        scheduler.shutdown();
        virtualThreadPool.shutdown();
        
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!virtualThreadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                virtualThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Interrupted while shutting down sweeper", e);
        }
        
        isRunning = false;
    }

    /**
     * Sweep for pending workflows across all tenants
     */
    private void sweepWorkflows() {
        List<String> tenants = TenantInfoHolder.getTenants();
        LOGGER.info("Sweeping workflows for " + tenants.size() + " tenants");
        
        for (String tenant : tenants) {
            try {
                TenantInfoHolder.setCurrentTenant(tenant);
                processTenantWorkflows(tenant);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing tenant: " + tenant, e);
            } finally {
                TenantInfoHolder.clearCurrentTenant();
            }
        }
    }

    /**
     * Process workflows for a specific tenant
     * 
     * @param tenant The tenant identifier
     */
    private void processTenantWorkflows(String tenant) {
        Connection connection = null;
        
        try {
            connection = dataSource.getConnection();
            WorkflowExecutionQueueDAO queueDAO = new WorkflowExecutionQueueDAO(connection);
            
            if (!isFirstSweep) {
                lastPollTime = queueDAO.getLatestPollTime();
            }

            List<String> workflowExecutionIds = queueDAO.fetchQueuedWorkflows(!isFirstSweep);
            LOGGER.info("Processing " + workflowExecutionIds.size() + " workflows for tenant: " + tenant);

            for (String workflowId : workflowExecutionIds) {
                if (threadLimitSemaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                    virtualThreadPool.execute(() -> {
                        Connection threadConnection = null;
                        try {
                            TenantInfoHolder.setCurrentTenant(tenant);
                            threadConnection = dataSource.getConnection();
                            WorkflowStateManager.decide(workflowId, threadConnection, lastPollTime);
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Error processing workflow: " + workflowId, e);
                        } finally {
                            TenantInfoHolder.clearCurrentTenant();
                            if (threadConnection != null) {
                                try {
                                    threadConnection.close();
                                } catch (Exception e) {
                                    LOGGER.log(Level.WARNING, "Error closing connection", e);
                                }
                            }
                            threadLimitSemaphore.release();
                        }
                    });
                } else {
                    LOGGER.warning("Max virtual thread limit reached. Skipping workflow execution: " + workflowId);
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing tenant workflows: " + tenant, e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error closing connection", e);
                }
            }
        }
    }
    
    /**
     * Force an immediate sweep of all tenants
     */
    public void forceSweep() {
        CompletableFuture.runAsync(() -> {
            try {
                sweepWorkflows();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in forced sweep", e);
            }
        });
    }
    
    /**
     * Check if the sweeper is currently running
     * 
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }
}

/**
 * This is a placeholder for the multi-tenant datasource.
 * In a real implementation, this would be provided by the host application.
 */
class APMultiTenantDatasource {
    public Connection getConnection() {
        return null; // In a real implementation, this would return a connection for the current tenant
    }
}

/**
 * This is a placeholder for the tenant information holder.
 * In a real implementation, this would be provided by the host application.
 */
class TenantInfoHolder {
    public static List<String> getTenants() {
        return List.of("tenant1", "tenant2"); // In a real implementation, this would return the actual tenants
    }
    
    public static void setCurrentTenant(String tenant) {
        // In a real implementation, this would set the current tenant
    }
    
    public static void clearCurrentTenant() {
        // In a real implementation, this would clear the current tenant
    }
}