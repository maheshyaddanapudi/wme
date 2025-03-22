package com.workday.pwe.enums;

/**
 * Represents the possible states of a task instance.
 */
public enum TaskStatus {
    /**
     * Task is created but not yet started
     */
    NOT_STARTED,
    
    /**
     * Task is currently being processed
     */
    IN_PROGRESS,
    
    /**
     * Generic completion status for tasks
     */
    COMPLETED,
    
    /**
     * Specific completion status for Submit tasks
     */
    SUBMITTED,
    
    /**
     * Specific completion status for Approve tasks
     */
    APPROVED,
    
    /**
     * Specific completion status for Review tasks
     */
    REVIEWED,
    
    /**
     * Specific completion status for HTTP tasks
     */
    API_CALL_COMPLETE,
    
    /**
     * Task execution encountered an error
     */
    FAILED,
    
    /**
     * Task was not processed within the allowed time
     */
    EXPIRED,
    
    /**
     * Task was manually bypassed
     */
    SKIPPED,
    
    /**
     * Task is waiting for other tasks to complete
     */
    BLOCKED
}