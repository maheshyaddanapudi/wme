package com.workday.pwe.enums;

/**
 * Represents the possible states of a workflow execution queue entry.
 */
public enum QueueStatus {
    /**
     * Entry is waiting to be processed
     */
    PENDING,
    
    /**
     * Entry is currently being processed
     */
    PROCESSING,
    
    /**
     * Entry was processed successfully
     */
    COMPLETED,
    
    /**
     * Processing encountered an error
     */
    FAILED
}