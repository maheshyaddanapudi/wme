package com.workday.pwe.enums;

/**
 * Represents the possible types of tasks.
 */
public enum TaskType {
    /**
     * To-Do task for simple human actions
     */
    TODO,
    
    /**
     * Submit task for submitting work products
     */
    SUBMIT,
    
    /**
     * Approve task for approval workflows
     */
    APPROVE,
    
    /**
     * Review task for review workflows
     */
    REVIEW,
    
    /**
     * HTTP task for system-to-system integrations
     */
    HTTP
}