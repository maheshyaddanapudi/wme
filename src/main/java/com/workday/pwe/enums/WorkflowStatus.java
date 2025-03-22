package com.workday.pwe.enums;

/**
 * Represents the possible states of a workflow instance.
 */
public enum WorkflowStatus {
    /**
     * Initial state when a workflow is created but not yet started
     */
    NOT_STARTED,
    
    /**
     * Workflow is actively executing and processing tasks
     */
    RUNNING,
    
    /**
     * Workflow execution is temporarily suspended
     */
    PAUSED,
    
    /**
     * All tasks in the workflow have been completed successfully
     */
    COMPLETED,
    
    /**
     * Workflow has encountered a critical error and cannot continue
     */
    FAILED,
    
    /**
     * Workflow was manually stopped before completion
     */
    TERMINATED,
    
    /**
     * Workflow has been moved to archive storage
     */
    ARCHIVED
}