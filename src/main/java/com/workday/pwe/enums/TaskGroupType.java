package com.workday.pwe.enums;

/**
 * Represents the possible types of task groups.
 */
public enum TaskGroupType {
    /**
     * Sequential execution - tasks execute one after another in order
     */
    VERTICAL,
    
    /**
     * Parallel execution - tasks execute simultaneously
     */
    HORIZONTAL
}