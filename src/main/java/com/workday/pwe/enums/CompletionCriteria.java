package com.workday.pwe.enums;

/**
 * Represents the criteria for determining when a task group is considered complete.
 */
public enum CompletionCriteria {
    /**
     * All tasks/groups within the group must complete
     */
    ALL,
    
    /**
     * Any one task/group within the group must complete
     */
    ANY,
    
    /**
     * A specific number of tasks/groups must complete
     */
    N_OF_M
}