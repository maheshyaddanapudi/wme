# Planning Workflow Engine - Technical Design Document

## 1. Introduction

### 1.1 Purpose
The Planning Workflow Engine (PWE) is a Java library designed to support configurable and scalable task management for workflow execution. It is intended to be embedded within a host monolithic application (specifically Workday Adaptive Planning) while maintaining a generic design to support various workflow patterns.

### 1.2 Scope
The PWE provides a comprehensive framework for:
- Managing workflow definitions and instances
- Creating and executing task groups and individual tasks
- Supporting different task types (human tasks and system tasks)
- Handling both sequential and parallel execution patterns
- Providing state management capabilities
- Enabling workflow versioning and migration

### 1.3 Design Goals
- **Database-Driven**: Full persistence of workflow state and execution
- **Modularity**: Clear separation of concerns between components
- **Scalability**: Support for high-throughput workflow processing
- **Flexibility**: Configurable task types and execution patterns
- **Extensibility**: Easy addition of new task types and operators

## 2. System Architecture

### 2.1 High-Level Architecture

The PWE follows a layered architecture pattern consisting of:

1. **API Layer**: Exposes Java interfaces for workflow operations
2. **Service Layer**: Implements business logic and orchestration
3. **Core Components Layer**: Handles task execution and state management
4. **Data Access Layer**: Manages database interactions
5. **Database**: Provides persistence via PostgreSQL

### 2.2 Component Breakdown

#### 2.2.1 API Layer
- **WorkflowDefinitionAPI**: Manages workflow definitions
- **WorkflowExecutionAPI**: Controls workflow execution
- **TaskManagementAPI**: Handles task operations
- **QueryAndReportAPI**: Provides search and reporting capabilities

#### 2.2.2 Service Layer
- **WorkflowDefinitionService**: Creates and updates workflow definitions
- **WorkflowInstanceService**: Manages workflow instances
- **WorkflowVersionService**: Handles workflow versioning
- **TaskInstanceService**: Creates and retrieves task instances
- **TaskCompletionService**: Handles task completion logic
- **WorkflowControlService**: Controls workflow state (pause, resume, terminate)
- **HistoryAndAuditService**: Tracks workflow and task state changes

#### 2.2.3 Core Components
- **Task Handlers**: Type-specific execution logic for tasks
  - ToDoTaskHandler
  - SubmitTaskHandler
  - ApproveTaskHandler
  - ReviewTaskHandler
  - HttpTaskHandler
- **TaskHandlerRegistry**: Maps task types to handlers
- **Task Group Handlers**: Manage groups of tasks
  - VerticalGroupHandler (sequential)
  - HorizontalGroupHandler (parallel)
- **WorkflowStateManager**: Decides next steps in workflow execution
- **ExecutionQueuingInterceptor**: Queues workflows for state management
- **WorkflowExecutionSweeper**: Periodically processes queued workflows
- **WorkflowJsonParser**: Parses and validates workflow definitions

#### 2.2.4 Data Access Layer
- **WorkflowDefinitionDAO**: Accesses workflow definitions
- **WorkflowInstanceDAO**: Manages workflow instances
- **TaskGroupDefinitionDAO**: Handles task group definitions
- **TaskGroupInstanceDAO**: Manages task group instances
- **TaskDefinitionDAO**: Handles task definitions
- **TaskInstanceDAO**: Manages task instances
- **WorkflowExecutionQueueDAO**: Manages execution queue
- **WorkflowHistoryDAO**: Tracks history and audit information

### 2.3 Data Models
- **WorkflowDefinition**: Template for workflow execution
- **WorkflowInstance**: Runtime instance of a workflow
- **TaskGroupDefinition**: Definition of a task group
- **TaskGroupInstance**: Runtime instance of a task group
- **TaskDefinition**: Definition of a task
- **TaskInstance**: Runtime instance of a task
- **WorkflowExecutionQueue**: Queue entries for state management
- **WorkflowHistory**: Audit records for state changes

## 3. Database Design

### 3.1 Schema
The schema consists of the following tables:
- `workflow_definitions`: Stores workflow templates
- `workflow_instances`: Tracks running workflow instances
- `task_group_definitions`: Defines task group templates
- `task_definitions`: Stores task templates
- `task_group_instances`: Tracks running task groups
- `task_instances`: Stores running tasks
- `workflow_execution_queue`: Manages execution queue
- `workflow_history`: Records state change history

### 3.2 Entity Relationships
- One workflow definition can have multiple workflow instances
- One workflow definition can have multiple task group definitions and task definitions
- Task groups can be nested (parent-child relationships)
- Task groups can contain multiple tasks
- Tasks can be standalone or belong to a task group
- One workflow instance can have multiple task group instances and task instances

### 3.3 Key Database Design Considerations
- **Indexing**: Strategic indexes on frequently queried columns
- **JSON Storage**: Parameters and data stored as JSONB for flexibility
- **Optimized Queries**: Efficient SQL operations for high throughput
- **Transactional Integrity**: Operations maintain ACID properties
- **Foreign Key Relationships**: Enforce data integrity

## 4. Execution Model

### 4.1 Task Types
- **Human Tasks**:
  - **ToDo**: Simple task requiring human action
  - **Submit**: Task for submitting work products
  - **Approve**: Task requiring approval
  - **Review**: Task for reviewing work
- **System Tasks**:
  - **HTTP**: Task for making API calls to external systems

### 4.2 Task Group Types
- **Vertical**: Sequential execution of tasks/groups
- **Horizontal**: Parallel execution of tasks/groups

### 4.3 Completion Criteria
- **ALL**: All tasks must complete
- **ANY**: Any one task must complete
- **N_OF_M**: N out of M tasks must complete

### 4.4 Task State Machine
Task states include:
- **NOT_STARTED**: Initial state
- **IN_PROGRESS**: Task is being executed
- **BLOCKED**: Task is waiting for dependencies
- **COMPLETED**: Task has completed successfully
- **SUBMITTED**: Submit task has been submitted
- **APPROVED**: Approve task has been approved
- **REVIEWED**: Review task has been reviewed
- **API_CALL_COMPLETE**: HTTP task has completed
- **FAILED**: Task has failed
- **EXPIRED**: Task has timed out
- **SKIPPED**: Task has been bypassed

### 4.5 Workflow State Machine
Workflow states include:
- **NOT_STARTED**: Initial state
- **RUNNING**: Workflow is executing
- **PAUSED**: Workflow is temporarily suspended
- **COMPLETED**: Workflow has completed successfully
- **FAILED**: Workflow has failed
- **TERMINATED**: Workflow was manually stopped
- **ARCHIVED**: Workflow has been archived

### 4.6 Execution Process
1. **Workflow Initialization**:
   - Create workflow instance
   - Create task group instances
   - Create task instances
   - Set workflow to RUNNING

2. **Task Execution**:
   - Start eligible tasks (based on group type)
   - Update task status as they progress
   - Queue for state management on task completion

3. **State Management**:
   - WorkflowExecutionSweeper polls queue
   - WorkflowStateManager decides next steps
   - Start newly eligible tasks
   - Check for workflow completion

4. **Workflow Completion**:
   - Set workflow to COMPLETED when all tasks/groups complete
   - Set workflow to FAILED if critical error occurs

## 5. Implementation Details

### 5.1 Task Handler Implementation
- Abstract `TaskHandler` class with lifecycle methods:
  - `prepare()`: Setup before execution
  - `execute()`: Task-specific logic
  - `cleanup()`: Cleanup after execution
  - `completeAndClose()`: Transition to completion state

### 5.2 Task Group Handler Implementation
- Abstract `TaskGroupHandler` class with:
  - Group-specific execution logic
  - Group completion evaluation
  - Tasks/subgroups management

### 5.3 Workflow JSON Parsing
- Parse workflow definition JSON to create DB entries
- Support for nested task groups
- Parameter mapping for tasks and groups

### 5.4 Multi-Tenant Support
- WorkflowExecutionSweeper handles multiple tenants
- TenantInfoHolder tracks current tenant context
- Connection management per tenant

### 5.5 Execution Sweeper Implementation
- Uses virtual threads for efficient concurrent processing
- Controls thread pool size to avoid resource exhaustion
- Configurable sweep interval
- Transaction management per workflow

### 5.6 History and Audit Tracking
- Record all state changes
- Store details in JSON format
- Support for queries by entity or workflow

## 6. API Specifications

### 6.1 WorkflowDefinitionAPI
- `createDefinition(name, jsonDefinition)`: Create workflow template
- `getDefinition(id)`: Get workflow definition by ID
- `getDefinitionByName(name)`: Get latest definition by name
- `updateDefinition(id, jsonDefinition)`: Create new version
- `validateDefinition(jsonDefinition)`: Validate definition JSON

### 6.2 WorkflowExecutionAPI
- `startWorkflow(workflowDefId, inputJson)`: Start workflow
- `getWorkflow(id)`: Get workflow instance
- `pauseWorkflow(id)`: Pause workflow
- `resumeWorkflow(id)`: Resume workflow
- `terminateWorkflow(id, reason)`: Terminate workflow

### 6.3 TaskManagementAPI
- `getTaskDetails(taskId)`: Get task details
- `getTasksForWorkflow(workflowInstanceId)`: Get tasks for workflow
- `completeTask(taskId, outputJson)`: Complete a task
- `submitTask(taskId, outputJson)`: Submit a task
- `approveTask(taskId, outputJson)`: Approve a task
- `updateTaskAssignment(taskId, newAssignee)`: Update assignee

### 6.4 QueryAndReportAPI
- `queryWorkflows(status)`: Query workflows by status
- `queryTasks(status)`: Query tasks by status
- `queryTasksByAssignee(assignee)`: Query by assignee
- `queryOverdueTasks()`: Find overdue tasks
- `getAuditHistory(workflowInstanceId)`: Get audit history

## 7. Host Application Integration

### 7.1 Integration Points
- Database connection management
- User and group information provision
- Transaction control
- API invocation

### 7.2 Connection Management
- Host application provides connections
- Library uses provided connections for operations
- Commit control is externally managed

### 7.3 User Management
- User information provided via DTOs
- Host application handles authentication/authorization
- Library references users by ID/name only

## 8. Performance Considerations

### 8.1 Database Optimization
- Efficient indexes for common queries
- Batch operations for bulk updates
- Connection pooling management
- Query optimization

### 8.2 Execution Optimization
- Virtual threads for concurrent processing
- Thread pool size management
- Priority-based queue processing
- Task batching

### 8.3 Memory Management
- Efficient object creation and disposal
- Garbage collection considerations
- Connection handling

### 8.4 Scalability
- Support for high volume of workflows
- Handling concurrent workflow executions
- Resource utilization controls

## 9. Error Handling and Recovery

### 9.1 Exception Handling
- Graceful error handling at each layer
- Proper exception propagation
- Detailed error logging
- Workflow and task failure management

### 9.2 Retry Mechanism
- Support for task retries
- Configurable retry count and delay
- Failure handling strategies

### 9.3 Recovery
- Workflow state recovery after system failure
- Incomplete task detection and handling
- Transaction integrity maintenance

## 10. Testing Strategy

### 10.1 Unit Testing
- Component isolation testing
- Mock database interactions
- Logic verification

### 10.2 Integration Testing
- Component interaction testing
- Database operation verification
- Transaction management testing

### 10.3 System Testing
- End-to-end workflow execution
- Performance and load testing
- Resilience testing

## 11. Implementation Requirements

### 11.1 Environment
- Java 17 or higher
- PostgreSQL database
- External Tomcat deployment
- Non-Spring Boot host application

### 11.2 Dependencies
- Jackson for JSON processing
- JDBC for database access
- JUnit for testing
- SLF4J for logging

### 11.3 Package Structure
- Main package: `com.workday.pwe`
- Subpackages:
  - `api`: Public interfaces
  - `service`: Business logic
  - `dao`: Data access
  - `model`: Data models
  - `handler`: Task handlers
  - `execution`: Execution components
  - `enums`: Enumeration types
  - `util`: Utility classes

### 11.4 Coding Standards
- Clean, maintainable code
- Comprehensive documentation
- Proper error handling
- Efficient resource management
- Thorough testing

## 12. Future Extensions

### 12.1 Additional Task Types
- Timer tasks
- Decision tasks
- Custom task types

### 12.2 Advanced Operators
- Fork/join operations
- Dynamic fork
- Conditional branching

### 12.3 Integration Capabilities
- REST API integration
- Message queue integration
- Event-driven workflows

### 12.4 Monitoring and Metrics
- Performance tracking
- Execution statistics
- SLA monitoring

## 13. Conclusion

The Planning Workflow Engine provides a robust foundation for workflow management with a focus on flexibility, scalability, and extensibility. Its database-driven approach ensures reliable state management, while the modular architecture allows for future enhancements. The implementation follows best practices for Java library development and database interaction, making it suitable for integration with enterprise applications.
