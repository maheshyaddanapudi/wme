# Technical Enhancement Document: Replacing Task Groups with Operators

## 1. Introduction

### 1.1 Purpose

This document outlines the technical approach for replacing database-driven task groups with operators in the Planning Workflow Engine (PWE). This enhancement aims to simplify the architecture, increase flexibility, and reduce database dependencies while maintaining all existing workflow functionality.

### 1.2 Background

The current implementation uses database tables (`task_group_definitions` and `task_group_instances`) to store and manage task groups. This approach requires database operations for task group management and limits the flexibility of workflow execution.

### 1.3 Goals

- Replace database-driven task groups with JSON-based operators
- Maintain all existing functionality of horizontal and vertical task groups
- Support nested operators similar to the current nested task groups
- Maintain backward compatibility with existing workflow definitions
- Improve runtime flexibility and reduce database operations

## 2. High-Level Approach

### 2.1 Operator Concept

Operators are JSON-based components that define control flow logic within workflows. Unlike task groups, operators:
- Exist only in workflow definition JSON
- Do not have dedicated database tables
- Are interpreted and processed at runtime by the Workflow State Manager
- Generate tasks dynamically based on their configuration

### 2.2 Operator Types

We will implement the following operator types to replace existing task groups:

1. **DynamicFork** (replaces Horizontal Task Group)
   - Enables parallel execution of tasks
   - Can branch based on configurable properties
   - Supports different completion criteria (ALL, ANY, N_OF_M)

2. **Sequence** (replaces Vertical Task Group)
   - Ensures sequential execution of tasks
   - Each task starts only when the previous task completes
   - Maintains the same execution order as vertical task groups

### 2.3 Workflow Definition & Instance Changes

**Workflow Definition JSON:**
- Replace task group nodes with operator nodes
- Maintain nested structure capabilities
- Add operator-specific properties

**Workflow Instance:**
- Store operator state in the workflow instance JSON
- Track task execution progress per operator
- Maintain relationships between tasks and their parent operators

## 3. Detailed Implementation

### 3.1 Database Schema Changes

#### 3.1.1 Tables to Remove

- `task_group_definitions`
- `task_group_instances`

#### 3.1.2 Schema Modifications

- Add `operator_data` JSONB column to `workflow_instances` table to store operator state
- Update `task_instances` table to replace `task_group_instance_id` with `operator_id` VARCHAR field

```sql
-- Remove task group tables
DROP TABLE task_group_instances;
DROP TABLE task_group_definitions;

-- Modify workflow_instances table
ALTER TABLE workflow_instances ADD COLUMN operator_data JSONB;

-- Modify task_instances table
ALTER TABLE task_instances DROP COLUMN task_group_instance_id;
ALTER TABLE task_instances ADD COLUMN operator_id VARCHAR(255);
ALTER TABLE task_instances ADD COLUMN operator_type VARCHAR(50);
```

### 3.2 Core Component Changes

#### 3.2.1 New Components

1. **OperatorManager**
   - Manages operator state within workflow instances
   - Interprets operator definitions
   - Creates tasks based on operator logic
   - Evaluates operator completion criteria

```java
package com.workday.pwe.operator;

public class OperatorManager {
    /**
     * Initialize operator data for a new workflow instance
     */
    public void initializeOperators(WorkflowInstance workflow, JsonNode definitionJson);
    
    /**
     * Get operators that have tasks ready to start
     */
    public List<OperatorState> getOperatorsWithEligibleTasks(WorkflowInstance workflow);
    
    /**
     * Process a completed task for its parent operator
     */
    public void handleTaskCompletion(WorkflowInstance workflow, TaskInstance task);
    
    /**
     * Check if all operators in a workflow are complete
     */
    public boolean areAllOperatorsComplete(WorkflowInstance workflow);
    
    /**
     * Get the next tasks to create based on operator logic
     */
    public List<TaskInstance> getNextTasksToCreate(WorkflowInstance workflow, OperatorState operator);
}

### 3.6 Model Changes

#### 3.6.1 WorkflowInstance Changes

Update the `WorkflowInstance` model to include operator data:

```java
package com.workday.pwe.model;

public class WorkflowInstance {
    // Existing fields...
    private JsonNode operatorData;
    
    // Existing methods...
    
    public JsonNode getOperatorData() {
        return operatorData;
    }
    
    public void setOperatorData(JsonNode operatorData) {
        this.operatorData = operatorData;
    }
}
```

#### 3.6.2 TaskInstance Changes

Update the `TaskInstance` model to replace task group references with operator references:

```java
package com.workday.pwe.model;

public class TaskInstance {
    // Existing fields, removing taskGroupInstanceId
    private String operatorId;
    private String operatorType;
    
    // Existing methods...
    
    public String getOperatorId() {
        return operatorId;
    }
    
    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }
    
    public String getOperatorType() {
        return operatorType;
    }
    
    public void setOperatorType(String operatorType) {
        this.operatorType = operatorType;
    }
    
    // Replace isPartOfGroup with isPartOfOperator
    public boolean isPartOfOperator() {
        return operatorId != null;
    }
}
```

### 3.7 Migration Plan

To support existing workflows, we need a migration plan:

#### 3.7.1 Database Migration Script

Create a migration script to convert task groups to operators:

```sql
-- Step 1: Add new columns to task_instances
ALTER TABLE task_instances ADD COLUMN operator_id VARCHAR(255);
ALTER TABLE task_instances ADD COLUMN operator_type VARCHAR(50);

-- Step 2: Add operator_data column to workflow_instances
ALTER TABLE workflow_instances ADD COLUMN operator_data JSONB;

-- Step 3: Migrate task_group_instances to operator data
-- This is a complex operation that requires custom code
-- See MigrationService implementation below

-- Step 4: Update tasks with operator information
UPDATE task_instances ti
SET operator_id = tgi.id::text,
    operator_type = CASE 
                      WHEN tgd.group_type = 'VERTICAL' THEN 'sequence'
                      WHEN tgd.group_type = 'HORIZONTAL' THEN 'dynamicFork'
                    END
FROM task_group_instances tgi
JOIN task_group_definitions tgd ON tgi.task_group_def_id = tgd.id
WHERE ti.task_group_instance_id = tgi.id;

-- Step 5: After successful migration and update of application code,
-- we can remove the old columns and tables (not immediately)
-- ALTER TABLE task_instances DROP COLUMN task_group_instance_id;
-- DROP TABLE task_group_instances;
-- DROP TABLE task_group_definitions;
```

#### 3.7.2 MigrationService Implementation

Create a service to handle migration of existing workflows:

```java
package com.workday.pwe.migration;

public class OperatorMigrationService {
    /**
     * Migrate task groups to operators for all workflows
     */
    public void migrateAllWorkflows(Connection connection) throws Exception {
        WorkflowInstanceDAO workflowDAO = new WorkflowInstanceDAO(connection);
        List<WorkflowInstance> workflows = workflowDAO.getAllWorkflowInstances();
        
        for (WorkflowInstance workflow : workflows) {
            migrateWorkflow(connection, workflow);
        }
    }
    
    /**
     * Migrate task groups to operators for a specific workflow
     */
    public void migrateWorkflow(Connection connection, WorkflowInstance workflow) throws Exception {
        // Get all task group instances for the workflow
        TaskGroupInstanceDAO groupInstDAO = new TaskGroupInstanceDAO(connection);
        List<TaskGroupInstance> groupInsts = groupInstDAO.getTaskGroupsByWorkflowId(workflow.getId());
        
        // Create operator data structure
        ObjectNode operatorData = JsonNodeFactory.instance.objectNode();
        ArrayNode operators = JsonNodeFactory.instance.arrayNode();
        
        // Process each task group instance
        for (TaskGroupInstance groupInst : groupInsts) {
            // Get the task group definition
            TaskGroupDefinition groupDef = groupInstDAO.getTaskGroupDefinition(groupInst.getTaskGroupDefId());
            
            // Create operator state
            ObjectNode operatorState = JsonNodeFactory.instance.objectNode();
            operatorState.put("operatorId", groupInst.getId().toString());
            
            // Map group type to operator type
            String operatorType = groupDef.getGroupType() == TaskGroupType.VERTICAL ? "sequence" : "dynamicFork";
            operatorState.put("operatorType", operatorType);
            
            // Set completion status
            operatorState.put("isComplete", isTaskGroupComplete(groupInst));
            
            // Get tasks for this group
            TaskInstanceDAO taskInstDAO = new TaskInstanceDAO(connection);
            List<TaskInstance> tasks = taskInstDAO.getTaskInstancesByGroupId(groupInst.getId());
            
            // Add task IDs
            ArrayNode taskIds = JsonNodeFactory.instance.arrayNode();
            for (TaskInstance task : tasks) {
                taskIds.add(task.getId().toString());
            }
            operatorState.set("taskIds", taskIds);
            
            // Create state data based on operator type
            ObjectNode stateData = JsonNodeFactory.instance.objectNode();
            
            if (operatorType.equals("sequence")) {
                stateData.put("currentIndex", tasks.size()); // All tasks have been created
                
                // Determine last completed index
                int lastCompletedIndex = -1;
                for (int i = 0; i < tasks.size(); i++) {
                    if (isTaskComplete(tasks.get(i))) {
                        lastCompletedIndex = i;
                    } else {
                        break;
                    }
                }
                stateData.put("lastCompletedIndex", lastCompletedIndex);
            } else if (operatorType.equals("dynamicFork")) {
                stateData.put("tasksCreated", true);
                
                // Count completed and failed tasks
                int completedTasks = 0;
                int failedTasks = 0;
                for (TaskInstance task : tasks) {
                    if (isTaskSuccessful(task)) {
                        completedTasks++;
                    } else if (isTaskFailed(task)) {
                        failedTasks++;
                    }
                }
                stateData.put("completedTasks", completedTasks);
                stateData.put("failedTasks", failedTasks);
            }
            
            operatorState.set("stateData", stateData);
            
            // Add to operators array
            operators.add(operatorState);
        }
        
        operatorData.set("operators", operators);
        
        // Update workflow instance with operator data
        workflow.setOperatorData(operatorData);
        workflowDAO.updateWorkflowInstance(workflow);
    }
    
    private boolean isTaskGroupComplete(TaskGroupInstance groupInst) {
        return groupInst.getStatus() == TaskStatus.COMPLETED ||
               groupInst.getStatus() == TaskStatus.FAILED ||
               groupInst.getStatus() == TaskStatus.SKIPPED;
    }
    
    private boolean isTaskComplete(TaskInstance task) {
        return isTaskSuccessful(task) || isTaskFailed(task);
    }
    
    private boolean isTaskSuccessful(TaskInstance task) {
        TaskStatus status = task.getStatus();
        return status == TaskStatus.COMPLETED ||
               status == TaskStatus.SUBMITTED ||
               status == TaskStatus.APPROVED ||
               status == TaskStatus.REVIEWED ||
               status == TaskStatus.API_CALL_COMPLETE ||
               status == TaskStatus.SKIPPED;
    }
    
    private boolean isTaskFailed(TaskInstance task) {
        TaskStatus status = task.getStatus();
        return status == TaskStatus.FAILED ||
               status == TaskStatus.EXPIRED;
    }
}

### 3.5 WorkflowInstanceService Changes

The WorkflowInstanceService needs to be updated to initialize operators when starting a workflow:

```java
package com.workday.pwe.service;

public class WorkflowInstanceService {
    // Existing code...
    
    private final OperatorManager operatorManager = new OperatorManager();
    
    /**
     * Start a new workflow instance
     */
    public WorkflowInstance startWorkflow(Connection connection, UUID workflowDefId, JsonNode inputJson) throws Exception {
        try {
            // Check if the workflow definition exists
            WorkflowDefinitionDAO workflowDefDAO = new WorkflowDefinitionDAO(connection);
            WorkflowDefinition workflowDef = workflowDefDAO.getWorkflowDefinition(workflowDefId);
            
            if (workflowDef == null) {
                throw new IllegalArgumentException("Workflow definition not found: " + workflowDefId);
            }
            
            // Create the workflow instance
            WorkflowInstance workflowInst = new WorkflowInstance(workflowDefId, inputJson);
            
            // Initialize operators
            operatorManager.initializeOperators(workflowInst, workflowDef.getDefinitionJson());
            
            // Create task instances for top-level tasks (not in operators)
            createStandaloneTasks(connection, workflowInst, workflowDef);
            
            // Save workflow instance
            WorkflowInstanceDAO workflowInstDAO = new WorkflowInstanceDAO(connection);
            UUID workflowInstId = workflowInstDAO.createWorkflowInstance(workflowInst);
            workflowInst.setId(workflowInstId);
            
            // Start the workflow
            workflowInst.start();
            workflowInstDAO.updateWorkflowInstance(workflowInst);
            
            // Add to the execution queue for processing
            ExecutionQueuingInterceptor.queueForStateManagement(connection, workflowInstId);
            
            return workflowInst;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error starting workflow", e);
            throw e;
        }
    }
    
    /**
     * Create standalone tasks for a workflow
     */
    private void createStandaloneTasks(Connection connection, WorkflowInstance workflowInst, WorkflowDefinition workflowDef) throws SQLException {
        TaskDefinitionDAO taskDefDAO = new TaskDefinitionDAO(connection);
        TaskInstanceDAO taskInstDAO = new TaskInstanceDAO(connection);
        
        // Get top-level tasks (not associated with task groups)
        List<TaskDefinition> taskDefs = taskDefDAO.getTopLevelTasks(workflowDef.getId());
        
        // Create instances for standalone tasks
        for (TaskDefinition taskDef : taskDefs) {
            // Skip tasks that belong to operators (those have operatorId in parameters)
            JsonNode params = taskDef.getParametersJson();
            if (params != null && params.has("operatorId")) {
                continue;
            }
            
            // Get assignee from task definition parameters
            String assignee = null;
            if (params != null && params.has("assignee")) {
                assignee = params.get("assignee").asText();
            }
            
            TaskInstance taskInst = new TaskInstance(workflowInst.getId(), taskDef.getId(), assignee);
            
            // Set due date if specified in parameters
            if (params != null && params.has("dueDate")) {
                String dueDateStr = params.get("dueDate").asText();
                if (dueDateStr != null && !dueDateStr.isEmpty()) {
                    try {
                        taskInst.setDueDate(LocalDateTime.parse(dueDateStr));
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error parsing due date: " + dueDateStr, e);
                    }
                }
            }
            
            // Copy input parameters from definition to instance
            taskInst.setInputJson(params);
            
            // Create the task instance
            UUID taskInstId = taskInstDAO.createTaskInstance(taskInst);
            taskInst.setId(taskInstId);
        }
    }
}
```

2. **OperatorState**
   - Represents the runtime state of an operator
   - Stored as part of the workflow instance JSON

```java
package com.workday.pwe.operator;

public class OperatorState {
    private String operatorId;
    private String operatorType;
    private JsonNode config;
    private List<String> taskIds;
    private boolean isComplete;
    private Map<String, Object> stateData;
    
    // Getters and setters
}
```

3. **Operator Interface**
   - Interface for specific operator implementations

```java
package com.workday.pwe.operator;

public interface Operator {
    /**
     * Get operator type identifier
     */
    String getType();
    
    /**
     * Process operator definition to create initial state
     */
    OperatorState initialize(String operatorId, JsonNode definitionNode);
    
    /**
     * Get next tasks to create based on current state
     */
    List<TaskDefinition> getNextTasks(WorkflowInstance workflow, OperatorState state);
    
    /**
     * Handle task completion
     */
    void handleTaskCompletion(WorkflowInstance workflow, OperatorState state, TaskInstance task);
    
    /**
     * Check if the operator is complete
     */
    boolean isComplete(WorkflowInstance workflow, OperatorState state);
}
```

#### 3.2.2 Operator Implementations

1. **DynamicFork (Horizontal Task Group Replacement)**

```java
package com.workday.pwe.operator.impl;

public class DynamicForkOperator implements Operator {
    @Override
    public String getType() {
        return "dynamicFork";
    }
    
    @Override
    public OperatorState initialize(String operatorId, JsonNode definitionNode) {
        OperatorState state = new OperatorState();
        state.setOperatorId(operatorId);
        state.setOperatorType("dynamicFork");
        
        // Extract configuration from definition
        ObjectNode config = JsonNodeFactory.instance.objectNode();
        
        // Default to "assignmentGroup" if not specified
        String branchOn = definitionNode.has("branchOn") ? 
                          definitionNode.get("branchOn").asText() : 
                          "assignmentGroup";
        config.put("branchOn", branchOn);
        
        // Default to "ALL" if not specified
        String completionCriteria = definitionNode.has("completionCriteria") ? 
                                   definitionNode.get("completionCriteria").asText() : 
                                   "ALL";
        config.put("completionCriteria", completionCriteria);
        
        // Min completion (for N_OF_M criteria)
        if (definitionNode.has("minCompletion")) {
            config.put("minCompletion", definitionNode.get("minCompletion").asInt());
        } else {
            config.put("minCompletion", 1);
        }
        
        // Store task templates
        if (definitionNode.has("tasks") && definitionNode.get("tasks").isArray()) {
            config.set("tasks", definitionNode.get("tasks"));
        }
        
        state.setConfig(config);
        state.setTaskIds(new ArrayList<>());
        state.setComplete(false);
        
        // Initialize state data
        Map<String, Object> stateData = new HashMap<>();
        stateData.put("tasksCreated", false);
        stateData.put("completedTasks", 0);
        stateData.put("failedTasks", 0);
        state.setStateData(stateData);
        
        return state;
    }
    
    @Override
    public List<TaskDefinition> getNextTasks(WorkflowInstance workflow, OperatorState state) {
        // Check if tasks have already been created
        if ((boolean) state.getStateData().get("tasksCreated")) {
            return Collections.emptyList();
        }
        
        List<TaskDefinition> tasksToCreate = new ArrayList<>();
        JsonNode config = state.getConfig();
        String branchOn = config.get("branchOn").asText();
        
        // Get task templates
        JsonNode taskTemplates = config.get("tasks");
        
        // Process task templates and create tasks based on branchOn property
        if ("assignmentGroup".equals(branchOn)) {
            // Get assignment groups from workflow input
            JsonNode inputJson = workflow.getInputJson();
            if (inputJson != null && inputJson.has("assignmentGroups") && 
                inputJson.get("assignmentGroups").isArray()) {
                JsonNode groups = inputJson.get("assignmentGroups");
                
                // For each template, create a task per assignment group
                for (JsonNode taskTemplate : taskTemplates) {
                    for (JsonNode group : groups) {
                        TaskDefinition taskDef = createTaskFromTemplate(taskTemplate, group);
                        tasksToCreate.add(taskDef);
                    }
                }
            }
        } else {
            // Handle other branching properties
            // Implementation depends on specific branching logic
        }
        
        // Mark tasks as created
        state.getStateData().put("tasksCreated", true);
        
        return tasksToCreate;
    }
    
    @Override
    public void handleTaskCompletion(WorkflowInstance workflow, OperatorState state, TaskInstance task) {
        Map<String, Object> stateData = state.getStateData();
        
        // Update completed/failed task counters
        if (isTaskSuccessful(task)) {
            stateData.put("completedTasks", (int) stateData.get("completedTasks") + 1);
        } else {
            stateData.put("failedTasks", (int) stateData.get("failedTasks") + 1);
        }
        
        // Check if operator is complete
        if (isComplete(workflow, state)) {
            state.setComplete(true);
        }
    }
    
    @Override
    public boolean isComplete(WorkflowInstance workflow, OperatorState state) {
        JsonNode config = state.getConfig();
        String completionCriteria = config.get("completionCriteria").asText();
        Map<String, Object> stateData = state.getStateData();
        
        int completedTasks = (int) stateData.get("completedTasks");
        int totalTasks = state.getTaskIds().size();
        
        switch (completionCriteria) {
            case "ALL":
                return completedTasks == totalTasks;
                
            case "ANY":
                return completedTasks > 0;
                
            case "N_OF_M":
                int minRequired = config.get("minCompletion").asInt();
                return completedTasks >= minRequired;
                
            default:
                return false;
        }
    }
    
    private TaskDefinition createTaskFromTemplate(JsonNode template, JsonNode assignmentGroup) {
        // Logic to create task definition from template and assignment group
        // Implementation depends on specific task creation logic
    }
    
    private boolean isTaskSuccessful(TaskInstance task) {
        TaskStatus status = task.getStatus();
        return status == TaskStatus.COMPLETED ||
               status == TaskStatus.SUBMITTED ||
               status == TaskStatus.APPROVED ||
               status == TaskStatus.REVIEWED ||
               status == TaskStatus.API_CALL_COMPLETE ||
               status == TaskStatus.SKIPPED;
    }
}

### 3.4 WorkflowJsonParser Changes

The WorkflowJsonParser is responsible for parsing workflow definitions and needs to be updated to handle operators:

```java
package com.workday.pwe.execution;

public class WorkflowJsonParser {
    // Existing code...
    
    /**
     * Parse a workflow definition
     */
    public static WorkflowDefinition parseWorkflowDefinition(Connection connection, String workflowName, String jsonDefinition) {
        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(jsonDefinition);
            
            // Create workflow definition
            WorkflowDefinitionDAO workflowDAO = new WorkflowDefinitionDAO(connection);
            WorkflowDefinition workflowDef = new WorkflowDefinition(workflowName, rootNode);
            UUID workflowId = workflowDAO.createWorkflowDefinition(workflowDef);
            workflowDef.setId(workflowId);
            
            // Parse node hierarchy but don't create task group database entities
            parseNodeWithoutTaskGroups(connection, rootNode, workflowId, null, null);
            
            return workflowDef;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing workflow definition", e);
            throw new RuntimeException("Error parsing workflow definition", e);
        }
    }
    
    /**
     * Parse a node in the workflow definition without creating task group entities
     */
    private static void parseNodeWithoutTaskGroups(Connection connection, JsonNode node, UUID workflowId, 
                                                String parentOperatorId, List<TaskDefinition> taskDefs) {
        try {
            String nodeType = node.path("type").asText();
            String nodeId = node.path("id").asText();
            
            if ("operator".equals(nodeType)) {
                // This is an operator node, parse tasks within it
                String operatorType = node.path("operatorType").asText();
                JsonNode tasks = node.path("tasks");
                
                if (tasks.isArray()) {
                    for (JsonNode taskNode : tasks) {
                        TaskDefinition taskDef = parseTaskNode(taskNode, workflowId, nodeId, operatorType);
                        if (taskDefs != null) {
                            taskDefs.add(taskDef);
                        }
                    }
                }
                
                // Handle nested operators
                JsonNode children = node.path("children");
                if (children.isArray()) {
                    for (JsonNode child : children) {
                        parseNodeWithoutTaskGroups(connection, child, workflowId, nodeId, null);
                    }
                }
            } else if ("task".equals(nodeType)) {
                // This is a task node
                TaskDefinition taskDef = parseTaskNode(node, workflowId, parentOperatorId, null);
                if (taskDefs != null) {
                    taskDefs.add(taskDef);
                }
            } else if ("group".equals(nodeType)) {
                // Legacy group node, convert to appropriate operator type
                String groupType = node.path("groupType").asText("vertical");
                String operatorType = "vertical".equalsIgnoreCase(groupType) ? "sequence" : "dynamicFork";
                
                // Handle children/tasks within the group
                JsonNode children = node.path("children");
                if (children.isArray()) {
                    List<TaskDefinition> groupTasks = new ArrayList<>();
                    
                    for (JsonNode child : children) {
                        parseNodeWithoutTaskGroups(connection, child, workflowId, nodeId, groupTasks);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing node", e);
            throw new RuntimeException("Error parsing node", e);
        }
    }
    
    /**
     * Parse a task node
     */
    private static TaskDefinition parseTaskNode(JsonNode node, UUID workflowId, String operatorId, String operatorType) {
        try {
            TaskDefinitionDAO taskDAO = new TaskDefinitionDAO(connection);
            
            String taskId = node.path("id").asText();
            String taskTypeName = node.path("taskType").asText();
            String taskName = node.path("name").asText();
            
            // Determine task type
            TaskType taskType;
            try {
                taskType = TaskType.valueOf(taskTypeName.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Unknown task type: " + taskTypeName + ", defaulting to TODO");
                taskType = TaskType.TODO;
            }
            
            // Create task definition
            TaskDefinition taskDef = new TaskDefinition(workflowId, taskName, taskType, 0);
            
            // Store operator information in parameters
            ObjectNode parametersJson = OBJECT_MAPPER.createObjectNode();
            if (operatorId != null) {
                parametersJson.put("operatorId", operatorId);
            }
            if (operatorType != null) {
                parametersJson.put("operatorType", operatorType);
            }
            
            // Copy other parameters from the task node
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (!List.of("id", "type", "taskType", "name").contains(fieldName)) {
                    parametersJson.set(fieldName, node.get(fieldName));
                }
            }
            
            taskDef.setParametersJson(parametersJson);
            
            // Create task in database
            UUID taskDefId = taskDAO.createTaskDefinition(taskDef);
            taskDef.setId(taskDefId);
            
            return taskDef;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing task", e);
            throw new RuntimeException("Error parsing task", e);
        }
    }
}
```

2. **Sequence (Vertical Task Group Replacement)**

```java
package com.workday.pwe.operator.impl;

public class SequenceOperator implements Operator {
    @Override
    public String getType() {
        return "sequence";
    }
    
    @Override
    public OperatorState initialize(String operatorId, JsonNode definitionNode) {
        OperatorState state = new OperatorState();
        state.setOperatorId(operatorId);
        state.setOperatorType("sequence");
        
        // Extract configuration from definition
        ObjectNode config = JsonNodeFactory.instance.objectNode();
        
        // Store task templates in order
        if (definitionNode.has("tasks") && definitionNode.get("tasks").isArray()) {
            config.set("tasks", definitionNode.get("tasks"));
        }
        
        state.setConfig(config);
        state.setTaskIds(new ArrayList<>());
        state.setComplete(false);
        
        // Initialize state data
        Map<String, Object> stateData = new HashMap<>();
        stateData.put("currentIndex", 0);
        stateData.put("lastCompletedIndex", -1);
        state.setStateData(stateData);
        
        return state;
    }
    
    @Override
    public List<TaskDefinition> getNextTasks(WorkflowInstance workflow, OperatorState state) {
        Map<String, Object> stateData = state.getStateData();
        int currentIndex = (int) stateData.get("currentIndex");
        int lastCompletedIndex = (int) stateData.get("lastCompletedIndex");
        
        // Check if we've moved beyond the last task
        JsonNode tasks = state.getConfig().get("tasks");
        if (currentIndex >= tasks.size()) {
            return Collections.emptyList();
        }
        
        // Verify the previous task is complete before moving to the next
        if (currentIndex > 0 && lastCompletedIndex < currentIndex - 1) {
            return Collections.emptyList();
        }
        
        // Get the next task template
        JsonNode taskTemplate = tasks.get(currentIndex);
        TaskDefinition taskDef = createTaskFromTemplate(taskTemplate);
        
        // Increment current index
        stateData.put("currentIndex", currentIndex + 1);
        
        return Collections.singletonList(taskDef);
    }
    
    @Override
    public void handleTaskCompletion(WorkflowInstance workflow, OperatorState state, TaskInstance task) {
        Map<String, Object> stateData = state.getStateData();
        
        // Find the task's index
        int taskIndex = findTaskIndex(state, task);
        if (taskIndex != -1) {
            int lastCompletedIndex = (int) stateData.get("lastCompletedIndex");
            if (taskIndex > lastCompletedIndex) {
                stateData.put("lastCompletedIndex", taskIndex);
            }
        }
        
        // Check if all tasks are complete
        JsonNode tasks = state.getConfig().get("tasks");
        if ((int) stateData.get("lastCompletedIndex") == tasks.size() - 1) {
            state.setComplete(true);
        }
    }
    
    @Override
    public boolean isComplete(WorkflowInstance workflow, OperatorState state) {
        Map<String, Object> stateData = state.getStateData();
        JsonNode tasks = state.getConfig().get("tasks");
        
        return (int) stateData.get("lastCompletedIndex") == tasks.size() - 1;
    }
    
    private TaskDefinition createTaskFromTemplate(JsonNode template) {
        // Logic to create task definition from template
        // Implementation depends on specific task creation logic
    }
    
    private int findTaskIndex(OperatorState state, TaskInstance task) {
        // Find the index of the task in the state's task list
        // Implementation depends on how tasks are tracked
    }
}

### 3.3 WorkflowStateManager Changes

The WorkflowStateManager is the central component that requires significant modifications to handle operators instead of task groups:

```java
package com.workday.pwe.execution;

public class WorkflowStateManager {
    // Existing code...
    
    // Add OperatorManager
    private static final OperatorManager operatorManager = new OperatorManager();
    
    /**
     * Main decision method that processes a workflow execution
     */
    public static void decide(String workflowId, Connection connection, Timestamp lastPollTime) {
        // Existing code for queue management...
        
        WorkflowInstance workflow = workflowDAO.getWorkflowInstance(UUID.fromString(workflowId));
        
        if (workflow == null || workflow.getStatus() != WorkflowStatus.RUNNING) {
            // Handle non-runnable workflow...
            return;
        }
        
        // Process completed tasks
        List<TaskInstance> completedTasks = getCompletedTasks(connection, workflow, lastPollTime);
        
        // Handle completed tasks with operators
        for (TaskInstance task : completedTasks) {
            processCompletedTask(connection, task);
        }
        
        // Start eligible tasks from operators
        startEligibleTasksFromOperators(connection, workflow);
        
        // Check workflow completion
        checkWorkflowCompletion(connection, workflow);
        
        // Remove from queue
        queueDAO.removeFromQueue(UUID.fromString(workflowId));
    }
    
    /**
     * Process a completed task and update its operator
     */
    private static void processCompletedTask(Connection connection, TaskInstance task) {
        if (task.getOperatorId() != null) {
            WorkflowInstance workflow = getWorkflowInstance(connection, task.getWorkflowInstanceId());
            operatorManager.handleTaskCompletion(workflow, task);
            
            // Update workflow instance with new operator state
            updateWorkflowInstance(connection, workflow);
        } else {
            // Handle standalone tasks (not part of an operator)
            // Existing logic...
        }
    }
    
    /**
     * Start eligible tasks from operators
     */
    private static void startEligibleTasksFromOperators(Connection connection, WorkflowInstance workflow) {
        List<OperatorState> eligibleOperators = operatorManager.getOperatorsWithEligibleTasks(workflow);
        
        for (OperatorState operatorState : eligibleOperators) {
            List<TaskInstance> tasksToStart = operatorManager.getNextTasksToCreate(workflow, operatorState);
            
            for (TaskInstance task : tasksToStart) {
                // Create task in database
                TaskInstanceDAO taskDAO = new TaskInstanceDAO(connection);
                task.setOperatorId(operatorState.getOperatorId());
                task.setOperatorType(operatorState.getOperatorType());
                UUID taskId = taskDAO.createTaskInstance(task);
                task.setId(taskId);
                
                // Add task ID to operator state
                operatorState.getTaskIds().add(taskId.toString());
                
                // Start the task
                TaskHandler.run(connection, task);
            }
        }
        
        // Update workflow instance with new operator state
        updateWorkflowInstance(connection, workflow);
    }
    
    /**
     * Check if workflow is complete
     */
    private static void checkWorkflowCompletion(Connection connection, WorkflowInstance workflow) {
        boolean allOperatorsComplete = operatorManager.areAllOperatorsComplete(workflow);
        
        if (allOperatorsComplete) {
            // Check for failures
            boolean anyOperatorFailed = operatorManager.hasAnyOperatorFailed(workflow);
            
            if (anyOperatorFailed) {
                workflowDAO.updateWorkflowStatus(workflow.getId(), WorkflowStatus.FAILED);
            } else {
                workflowDAO.updateWorkflowStatus(workflow.getId(), WorkflowStatus.COMPLETED);
            }
        }
    }
}
```

#### 3.2.3 OperatorRegistry

```java
package com.workday.pwe.operator;

public class OperatorRegistry {
    private static final Map<String, Operator> OPERATOR_MAP = new ConcurrentHashMap<>();
    
    static {
        // Register default operators
        registerOperator(new DynamicForkOperator());
        registerOperator(new SequenceOperator());
    }
    
    /**
     * Register an operator implementation
     */
    public static void registerOperator(Operator operator) {
        OPERATOR_MAP.put(operator.getType(), operator);
    }
    
    /**
     * Get operator by type
     */
    public static Operator getOperator(String type) {
        Operator operator = OPERATOR_MAP.get(type);
        if (operator == null) {
            throw new IllegalArgumentException("No operator registered for type: " + type);
        }
        return operator;
    }
    
    /**
     * Check if operator type is registered
     */
    public static boolean hasOperator(String type) {
        return OPERATOR_MAP.containsKey(type);
    }
}