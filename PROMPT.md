Let’s try to finish this in less number of iterations and we can leave out Rest API in this requirement as it’s going to be library and not stand alone. 

Goal: Research, Analyze, Understand existing Design and Develop remaining Planning Workflow Engine solution. 

1 - During research and analysis, we will understand and digest all the information keep in mind Workday Adaptive Planning Application will be the host consuming this library but our design should be remain generic. 

2 - During Design, we will do a concrete design l keeping in mind that the provided task types are examples of how there might be stateful automated tasks , stateless human tasks and with additional context from the research phase. We will not be making it specific to Workday adaptive Planning but we will keep in context of it being the host application. Our design and implementation outline is generic and should be kept so. We will reuse all the provided code. We are free to update db schema further. 

3 - We will create a least bit granular implementation todo list which we will keep updating and referring back to, to check where we left off and pick up from there. Avoiding missing steps like coding a specific file during development phase. 

4 - All necessary diagrams are attached. Let’s stick to them as much as possible. 
5 - During development phase we will think less and code more because by this point we would have designed a proper solution at granular level and we need is to code it out. 

Goal: Research, Analyze, Design and Develop this solution. 

Planning Workflow Engine - Fully Comprehensive Requirements Document

The Planning Workflow Engine (PWE) should support the execution of this workflow while maintaining configurable and scalable task management.

This document provides a detailed specification of the Workflow Management Engine, which is a fully database-reliant system designed for high-performance, modular, and scalable workflow automation.

⸻

1. Overall Architecture
	•	Java Library (JAR): The workflow engine will be developed as a standalone Java library (JAR) that integrates into an existing monolithic application which is non spring boot based and deploys on external tomcat.
	•	Database-Driven: Full reliance on a relational database, ensuring all workflow execution and state transitions are persisted. The database used by the host application is Postgres. 
	•	Poll based Sweeper and State Manager Execution: Tasks transition automatically upon completion, triggering subsequent tasks.
	•	API-Driven: The engine will expose Java APIs for workflow creation, execution, querying, and management.
	•	Workflow Versioning: Supports updating in-flight workflows without disrupting execution.

⸻

2. Workflow and Task Management

2.1 Task Types
	•	Predefined Task Types: HTTP task (non human system task)
	•	To-Do
	•	Submit
	•	Approve
	•	Review
	•	Expandable Task Model:
	•	New task types can be dynamically defined and linked to execution classes.

2.2 Terminal States
	•	Each task type must have predefined terminal states: At Java level in the classes. The base class defaults to COMPLETED which can be overridden in child classes to what makes a sensible completion status for its type. This should be considered when defining the db columns. Maybe we should be having a column to indicate if task is complete as we allow custom completion statuses. 
	•	Submitted → Submit Task
	•	Approved → Approval Task
	•	Completed → To-Do Task
	•	Configurable extension of terminal states to accommodate custom use cases.

2.3 Workflow Structure
	•	Initial Support: Mix of Parallel and Sequential Execution
	•	Expandable to:
	•	Fork
	•	Dynamic Fork
	•	JSON-Defined Workflows:
	•	Workflows must be configurable in JSON format for dynamic execution.

2.4 Task handling - Discussed later in document

3. Workflow Execution and Management

3.1 CRUD Operations
	•	Task Definition: Create, update, delete, retrieve task definitions.
	•	Task Instance: Manage runtime instances of tasks.
	•	Workflow Definition: Define and store workflows.
	•	Workflow Instance: Manage runtime execution of workflows.

3.2 Execution Control
	•	Bulk Operations:
	•	Batch processing of workflow transitions.
	•	The Workflow State Manager determines and triggers subsequent tasks.
	•	Task Resubmission:
	•	A task can be sent back to In Progress for rework.
	•	The system maintains a rework history.
	•	Workflow Control:
	•	Pause, Restart, Skip Tasks, Terminate, Rerun from Last Failed Task.

3.3 Dynamic Task actions
	•	Expose a service to adjust in-progress workflow instances when extra tasks should be added and queued for execution. This will be delegated by Workflow State Manager. 
	•	Triggering Mechanism:
	•	The host application provides a set of new tasks added in the existing workflow execution json and state manager validated and decides and updates the db and queues new tasks for execution following or within the hierarchy of workflow.
	•	The system must expose API to migrate existing running workflow instances to a new version catering for new task additions, existing parameter value updates which reflect at task parameter level (mostly just non execution logic dependent variables, if something impacts the execution of in flight tasks, it’s host application responsibility to retrigger those tasks, so maybe we can add a flag which allows retrigger all in flight tasks, then we need to cater for removal of tasks etc scenarios - all scenarios have to be explored and counted for in our design. 

Perfect—thanks for the clarification. Let’s now define everything generically first, laying a foundation for any workflow, and then we’ll apply that to your specific use case.

⸻

1. Core Workflow Units

A. Task (Atomic Unit)
	•	Definition: The most basic unit of execution in a workflow.
	•	Types:
	•	Human task (e.g., ToDo, Submit, Approve, Review)
	•	System task (e.g., sendEmail, validateData, triggerJob)
	•	Attributes:
	•	taskId
	•	taskType (custom-defined string or enum)
	•	assignee (user, user group, or system identity)
	•	inputParams, outputParams
	•	status, startTime, endTime, etc.
	•	Behavior:
	•	Executed once triggered by the engine
	•	Reports completion/failure
	•	Optionally can emit events

⸻

B. Task Group (Composite Unit)

Used to define a sequence or parallelism of task(s)/group(s).

i. Vertical Task Group (Sequential)
	•	Definition: Executes tasks or groups in a strict order
	•	Use Case: Submit → Approve, Validate → Trigger Email → Update DB
	•	Attributes:
	•	completionCriteria: ALL (default)
	•	tasks: ordered list of child task/group units

ii. Horizontal Task Group (Parallel)
	•	Definition: Executes all child tasks/groups in parallel
	•	Use Case: Parallel ToDo tasks, Multiple Submit-Approve flows
	•	Attributes:
	•	completionCriteria: ALL, ANY, N_OF_M
	•	tasks: unordered list of child task/group units

⸻

C. Workflow Definition (Workflow DAG)
	•	Definition: A directed graph (DAG) of task units and task groups
	•	Starting Node: Can be a task or group
	•	Execution Flow: Determined by grouping structure
	•	Reusability: Can be templatized and dynamic via metadata or input

⸻

2. Purpose of Different Workflow Units

Unit Type	Purpose
Task	Atomic work unit, human/system activity
Vertical Group	Ensures strict sequence of execution
Horizontal Group	Enables concurrent execution for scalability and speed

Why 3 types?
	•	They allow expressive orchestration:
	•	Nesting: Tasks can be grouped inside other groups
	•	Looping: Groups can be looped conditionally (e.g., resubmission)
	•	Reusability: Define task group templates

⸻

Summary of Execution Rules Applied
	•	Tasks = Any activity, not just human
	•	Task Groups = Used to enforce ordering or concurrency
	•	Completion criteria allow partial/conditional logic
	•	Nesting and chaining enable dynamic flows
	•	Workflow DSL allows any composition: task → group → group → task, etc.

⸻

4. Query & Reporting

4.1 Query Service
	•	Search across:
	•	Task Instances
	•	Workflow Executions
	•	Filtering by parameters and JSON content.

4.2 Dashboard APIs
	•	Provides admin functionalities:
	•	View workflow definitions and executions.
	•	Retrieve detailed execution logs.

4.3 History and Auditing
	•	Maintain a detailed log of state changes:
	•	Task transitions
	•	User actions
	•	Timestamps
	•	Audit Trail API for retrieving execution history.

⸻

5. API & Integration
	•	Java APIs:
	•	Workflow Management: Start, stop, pause, restart, terminate workflows.
	•	Task Management: Assign, complete, reject, resubmit tasks.
	•	Bulk Operations: Perform bulk updates and transitions.
	•	Query APIs: Search workflows and tasks dynamically. Also should allow in json searches by some lucene or based query or create a QueryRequestCreator.java to accept basic key value pair based list in string format that can be created using a builder().filter(k1,v1).filter(k2,v2).build() and it should also offer native column based filters like .name(username).status etc
	•	Database Connection Management:
	•	Support for passing a database connection object from the host application.
	•	Commit control is externally managed for transactional integrity.

⸻

6. Database & Performance Optimization

6.1 Efficient SQL for DML Operations
	•	Optimized for:
	•	Indexing
	•	Partitioning
	•	Batch processing
	•	Avoiding unnecessary locks
	•	Queries must be highly optimized due to full database reliance.

6.2 SQL Schema
	•	Tables:
	•	workflow_definitions
	•	workflow_instances
	•	task_group_definition - should have a nullable task_group_definition_id (non null if part of a group) No need for a separate parent task group id logic as its nested
	•	task_group_execution - should have a nullable task_group_execution_id (non null if part of a group) No need for a separate parent task group id logic as its nested
	•	task_definitions - should have a nullable task_group_definition_id (non null if part of a group). No need for a separate parent task group id logic as its nested
	•	task_instances - should have a nullable task_group_execution_id (non null if part of a group) No need for a separate parent task group id logic as its nested
	•	workflow_history
	•	workflow_execution_queue
	•	Supports JSON-based attributes.

Check the Core Execution Logic section if required, while designing the database schema. 

6.3 Data Retention & Cleanup
	•	Configurable retention policies.
	•	Auto-archiving of workflow history and logs.

⸻

7. Extensibility & Future-Proofing
	•	Operator Expandability:
	•	Fork, Dynamic Fork, etc.
	•	Modular & Configurable:
	•	Minimize hard-coded logic and allow runtime configuration.

⸻

8. Example Workflow - Quarterly Planning Workflow 

8.1 General representation of the workflow (This is for context and doesn’t comply with our workflow definition)

{
  "workflowId": "quarterly_planning",
  "name": "Quarterly Planning Workflow",
  "tasks": [
    {
      "taskId": "task_1",
      "type": "Submit",
      "assignee": "user_group:finance_team",
      "stateTransitions": {
        "Submitted": "task_2"
      }
    },
    {
      "taskId": "task_2",
      "type": "Approve",
      "assignee": "user:finance_manager",
      "stateTransitions": {
        "Approved": "task_3",
        "Rejected": "task_1"
      }
    },
    {
      "taskId": "task_3",
      "type": "Review",
      "assignee": "user:executive_board",
      "stateTransitions": {
        "Reviewed": "Complete"
      }
    }
  ]
}

Maybe a sample workflow definition in our system could look like this (very minimal example)

{
  "id": "root",
  "type": "group",
  "groupType": "horizontal",
  "completionCriteria": "ALL",
  "children": [
    {
      "id": "level1",
      "type": "group",
      "groupType": "horizontal",
      "completionCriteria": "ALL",
      "children": [
        {
          "id": "level1_todo",
          "type": "group",
          "groupType": "horizontal",
          "completionCriteria": "ANY",
          "children": [
            { "id": "todo_user1", "type": "task", "taskType": "ToDo", "assignee": "user1" },
            { "id": "todo_user2", "type": "task", "taskType": "ToDo", "assignee": "user2" }
          ]
        },
        {
          "id": "level1_submit_approve",
          "type": "group",
          "groupType": "horizontal",
          "completionCriteria": "ALL",
          "children": [
            {
              "id": "sa1",
              "type": "group",
              "groupType": "vertical",
              "children": [
                { "id": "submit1", "type": "task", "taskType": "Submit", "assignee": "submitter1" },
                { "id": "approve1", "type": "task", "taskType": "Approve", "assignee": "approver1" }
              ]
            }
          ]
        },
        {
          "id": "level1_review",
          "type": "group",
          "groupType": "horizontal",
          "completionCriteria": "ANY",
          "children": [
            { "id": "review1", "type": "task", "taskType": "Review", "assignee": "reviewer1" }
          ]
        }
      ]
    }
  ]
}

⸻

8.2 Runtime Example
(Enhance this to include task group based and individual task based mixed approach)

Initial State

Task ID	Type	Assignee	State
task_1	Submit	user_1 (finance_team)	Pending
task_1	Submit	user_2 (finance_team)	Pending
task_1	Submit	user_3 (finance_team)	Pending
task_2	Approve	finance_manager	Not Started
task_3	Review	executive_board	Not Started

After user_1 submits

Task ID	Type	Assignee	State
task_1	Submit	user_1 (finance_team)	Submitted
task_1	Submit	user_2 (finance_team)	Pending
task_1	Submit	user_3 (finance_team)	Pending
task_2	Approve	finance_manager	Not Started
task_3	Review	executive_board	Not Started

After all users submit

Task ID	Type	Assignee	State
task_1	Submit	user_1 (finance_team)	Submitted
task_1	Submit	user_2 (finance_team)	Submitted
task_1	Submit	user_3 (finance_team)	Submitted
task_2	Approve	finance_manager	Pending
task_3	Review	executive_board	Not Started

After Approval

Task ID	Type	Assignee	State
task_1	Submit	-	Completed
task_2	Approve	finance_manager	Approved
task_3	Review	executive_board	Pending

After Review Completion

Task ID	Type	Assignee	State
task_3	Review	executive_board	Reviewed
			Workflow Complete



3. Applying Task & Task Group to the Use Case

Let’s reapply our example with generic terminology and flexible structure:

Full DAG Expansion (Abstract View)

Root
	•	Type: Horizontal Task Group (completionCriteria = ALL)
	•	Children:
	•	Level 1 Block
	•	Level 2 Block
	•	Level 3 Block

⸻

Level 1 Block
	•	Type: Horizontal Task Group (completionCriteria = ALL)
	•	Children:
	•	ToDo Group
	•	Type: Horizontal Task Group (completionCriteria = ANY)
	•	Children:
	•	Task: ToDo Task (User 1)
	•	Task: ToDo Task (User 2)
	•	Task: ToDo Task (User 3)
	•	SubmitApprove Group
	•	Type: Horizontal Task Group (completionCriteria = ALL)
	•	Children (3 pairs):
	•	Vertical Group 1:
	•	Submit Task (User A1)
	•	Approve Task (User B1)
	•	Vertical Group 2:
	•	Submit Task (User A2)
	•	Approve Task (User B2)
	•	Vertical Group 3:
	•	Submit Task (User A3)
	•	Approve Task (User B3)
	•	Review Group
	•	Type: Horizontal Task Group (completionCriteria = ANY)
	•	Children:
	•	Task: Review Task (User R1)
	•	Task: Review Task (User R2)

⸻

Level 2 Block
	•	Type: Vertical Task Group (completionCriteria = ALL)
	•	Children:
	•	Task: ToDo Task (User X)
	•	SubmitApprove Group
	•	Type: Horizontal Task Group (completionCriteria = ALL)
	•	Children (2 pairs):
	•	Vertical Group 1:
	•	Submit Task (User A1)
	•	Approve Task (User B1)
	•	Vertical Group 2:
	•	Submit Task (User A2)
	•	Approve Task (User B2)
	•	Task: Review Task (User R)

⸻

Level 3 Block
	•	Type: Vertical Task Group (completionCriteria = ALL)
	•	Children:
	•	ToDo Group
	•	Type: Horizontal Task Group (completionCriteria = ANY)
	•	Children:
	•	Task: ToDo Task (User 1)
	•	Task: ToDo Task (User 2)
	•	SubmitApprove Group
	•	Type: Vertical Task Group (completionCriteria = ALL)
	•	Children:
	•	Submit Task (User A)
	•	Approve Task (User B)
	•	Review Group
	•	Type: Horizontal Task Group (completionCriteria = ANY)
	•	Children:
	•	Task: Review Task (User R1)
	•	Task: Review Task (User R2)

⸻

9. Summary

This document comprehensively defines:
	•	The architecture and database structure.
	•	Efficient SQL optimization.
	•	Workflow execution logic with real-time state updates.

The schema design definitely and architectural design even the Workflow level details or task level details , like a retryCount and retryDelay etc to be supported in the parameters. Maybe even a duration for a task which gets timed at runtime and gets expired after X hours or Y Days etc. which usually any workflow engine has and further such properties
The document only provides basic details. 

We also need to cater for how at run time/trigger time dynamic values, for example a level and an association can be given and it should get mapped to corresponding task inputs. Kind of during definition phase like dynamic mapping of all task inputs to workflow inputs in such a way that during execution phase user provides all workflow inputs which get cascaded to corresponding mapped task inputs. This also should be catered to when updating an existing running workflow execution to latest definition and making adjustments. 

For the users and user groups, assume a dto structure. We don’t need db tables because those will be in host application. We will only get user and user group details from host application
*VERY IMPORTANT FOR DYNAMIC FIELDS SPECIFIC TO TASK TYPES* Also keep in mind these associations or assignments are specific only to the current task types. We might have some other fields for future task types. So these should be loosely coupled may be in a json at db level but at Java level, we can tight coupe them to the task types. 
Also the task type to Java handler class should be map maintained in Java layer for where ever reference required. 

Every resource or service exposed should take in a connection object in their method calls from host application. Cannot be pre coupled during any initialization. Let’s imagine it this way, the host application provided a connection object with auto commit off to our api engine or services which drill down to DAO and further. The host application wants to control if and when they commit that connection.

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Periodically sweeps pending workflows for execution per tenant.
 */
public class WorkflowExecutionSweeper {

    private static final Logger LOGGER = Logger.getLogger(WorkflowExecutionSweeper.class.getName());

    private static final int MAX_VIRTUAL_THREADS;
    private static final int SWEEP_INTERVAL_MS = 10_000; // 10 seconds
    private static boolean isFirstSweep = true;
    private static Timestamp lastPollTime = null;

    static {
        Properties properties = new Properties();
        MAX_VIRTUAL_THREADS = Integer.parseInt(properties.getProperty("max.virtual.threads", "50"));
    }

    private final ExecutorService virtualThreadPool = Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore threadLimitSemaphore = new Semaphore(MAX_VIRTUAL_THREADS);
    private final APMultiTenantDatasource dataSource;

    private WorkflowExecutionSweeper() {
        this.dataSource = new APMultiTenantDatasource();
    }

    public void init() {
        LOGGER.info("Starting WorkflowExecutionSweeper with max virtual threads: " + MAX_VIRTUAL_THREADS);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                sweepWorkflows();
                isFirstSweep = false;
            } catch (Exception e) {
                LOGGER.severe("Error in WorkflowExecutionSweeper loop: " + e.getMessage());
            }
        }, 0, SWEEP_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void sweepWorkflows() {
        for (String tenant : TenantInfoHolder.getTenants()) {
            try {
                TenantInfoHolder.setCurrentTenant(tenant);
                processTenantWorkflows();
            } finally {
                TenantInfoHolder.clearCurrentTenant();
            }
        }
    }

    private void processTenantWorkflows() {
        try (Connection connection = dataSource.getConnection()) {
            WorkflowExecutionManagerQueueDAO queueDAO = new WorkflowExecutionManagerQueueDAO(connection);
            
            if (!isFirstSweep) {
                lastPollTime = queueDAO.getLatestPollTime();
            }

            List<String> workflowExecutionIds = queueDAO.fetchQueuedWorkflows(!isFirstSweep);
            LOGGER.info("Processing workflows after lastPollTime: " + lastPollTime);

            for (String workflowId : workflowExecutionIds) {
                if (threadLimitSemaphore.tryAcquire()) {
                    virtualThreadPool.execute(() -> {
                        try {
                            WorkflowStateManager.decide(workflowId, connection, lastPollTime);
                        } finally {
                            threadLimitSemaphore.release();
                        }
                    });
                } else {
                    LOGGER.warning("Max virtual thread limit reached. Skipping workflow execution: " + workflowId);
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Error processing tenant workflows: " + e.getMessage());
        }
    }
}

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations related to workflow execution queue.
 */
public class WorkflowExecutionManagerQueueDAO {

    private final Connection connection;

    public WorkflowExecutionManagerQueueDAO(Connection connection) {
        this.connection = connection;
    }

    public Timestamp getLatestPollTime() {
        String query = "SELECT MAX(last_updated) FROM workflow_execution_queue WHERE status = 'PROCESSING'";

        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getTimestamp(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving latest poll time", e);
        }
        return null;
    }

    public List<String> fetchQueuedWorkflows(boolean includeProcessing) {
        List<String> workflows = new ArrayList<>();
        String query = "SELECT workflow_execution_id FROM workflow_execution_queue WHERE status = 'PENDING'"
                     + (includeProcessing ? " OR status = 'PROCESSING'" : "");

        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                workflows.add(rs.getString("workflow_execution_id"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching queued workflows", e);
        }
        return workflows;
    }

    public void updateQueueStatus(String workflowExecutionId, QueueStatus status) {
        String query = "UPDATE workflow_execution_queue SET status = ? WHERE workflow_execution_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, status.name());
            stmt.setString(2, workflowExecutionId);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error updating queue status", e);
        }
    }

    public void removeFromQueue(String workflowExecutionId) {
        String query = "DELETE FROM workflow_execution_queue WHERE workflow_execution_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, workflowExecutionId);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error removing workflow from queue", e);
        }
    }
}

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

/**
 * Handles workflow execution-related database operations.
 */
public class WorkflowExecutionDAO {

    private final Connection connection;

    public WorkflowExecutionDAO(Connection connection) {
        this.connection = connection;
    }

    public boolean isWorkflowInProgress(String workflowExecutionId) {
        String query = "SELECT COUNT(1) FROM workflows WHERE execution_id = ? AND status IN ('IN_PROGRESS', 'PAUSED')";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, workflowExecutionId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            throw new RuntimeException("Error checking workflow status", e);
        }
    }

    public List<TaskInstance> getCompletedTasks(String workflowExecutionId) {
        // Implementation for retrieving completed tasks for workflow execution
    }
}

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;

/**
 * Decides the next workflow execution step based on completed tasks.
 */
public class WorkflowStateManager {

    private static final Logger LOGGER = Logger.getLogger(WorkflowStateManager.class.getName());

    public static void decide(String workflowExecutionId, Connection connection, Timestamp lastPollTime) {
        WorkflowExecutionDAO executionDAO = new WorkflowExecutionDAO(connection);
        WorkflowExecutionManagerQueueDAO queueDAO = new WorkflowExecutionManagerQueueDAO(connection);

        try {
            queueDAO.updateQueueStatus(workflowExecutionId, QueueStatus.PROCESSING);

            if (!executionDAO.isWorkflowInProgress(workflowExecutionId)) {
                queueDAO.removeFromQueue(workflowExecutionId);
                return;
            }

            List<TaskInstance> completedTasks = (lastPollTime != null)
                ? executionDAO.getCompletedTasksAfter(workflowExecutionId, lastPollTime)
                : getRelevantCompletedTasks(executionDAO.getCompletedTasks(workflowExecutionId), executionDAO);

            for (TaskInstance completedTask : completedTasks) {
                if (completedTask.isTaskGroup()) {
                    new TaskGroupHandler(connection, completedTask).execute(connection);
                } else {
                    TaskHandler.run(connection, completedTask);
                }
            }

            queueDAO.removeFromQueue(workflowExecutionId);

        } catch (Exception e) {
            LOGGER.severe("Error processing WorkflowExecution [" + workflowExecutionId + "]: " + e.getMessage());
        }
    }
}

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Abstract class representing a task handler in a workflow execution system.
 * Handles task execution lifecycle and transitions.
 */
public abstract class TaskHandler {

    private static final Logger LOGGER = Logger.getLogger(TaskHandler.class.getName());

    private final TaskInstance taskInstance;
    private Connection connection; // Connection will be set using setter method if the task type is stateful. 

    protected TaskHandler(TaskInstance taskInstance) {
        if (taskInstance == null) {
            throw new IllegalArgumentException("TaskInstance cannot be null");
        }
        this.taskInstance = taskInstance;
    }

    protected boolean isStateful() {
        return false;
    }

    protected void setConnection(Connection connection) {
        this.connection = connection;
    }

    private final void moveToInProgress(Connection connection) {
        try {
            new TaskDAO(connection).updateTask(taskInstance.getID(), TaskStatus.IN_PROGRESS);
            LOGGER.info("Task moved to IN_PROGRESS: " + taskInstance.getID());
        } catch (Exception e) {
            throw new RuntimeException("Error updating task to IN_PROGRESS", e);
        }
    }

    private final void moveToComplete(Connection connection, String reasonForFailure) {
        try {
            TaskDAO taskDAO = new TaskDAO(connection);
            if (reasonForFailure == null) {
                taskDAO.updateTask(taskInstance.getID(), getCompletionStatus());
                LOGGER.info("Task completed successfully: " + taskInstance.getID());
            } else {
                taskDAO.updateTask(taskInstance.getID(), getFailureStatus(), reasonForFailure);
                LOGGER.warning("Task failed: " + taskInstance.getID() + ", Reason: " + reasonForFailure);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error updating task completion state", e);
        }
    }

    protected void prepare(Connection connection) {}

    protected void cleanup(Connection connection) { 

    }

    protected abstract void execute(Connection connection);

    protected abstract String getTaskType();

    protected TaskStatus getCompletionStatus() {
        return TaskStatus.COMPLETE;
    }

    protected TaskStatus getFailureStatus() {
        return TaskStatus.FAILED;
    }

    public static void run(Connection connection, TaskInstance taskInstance) {
        TaskHandler handler = TaskHandlerRegistry.getHandler(connection, taskInstance);
        handler.prepare(connection);
        handler.moveToInProgress(connection);
        handler.execute(handler.getConnection());
    }

    public static void completeAndClose(Connection connection, TaskInstance taskInstance, String reasonForFailure) {
        TaskHandler handler = TaskHandlerRegistry.getHandler(connection, taskInstance);
        handler.moveToComplete(connection, reasonForFailure);
        handler.cleanup(connection);
        handler.close(connection);
    }

    private final void close(Connection connection) {
        List<String> workflowExecutionIds = Collections.singletonList(taskInstance.getWorkflowExecutionId());
        ExecutionQueuingInterceptor.queueForStateManagement(connection, workflowExecutionIds);
        LOGGER.info("Task closed and queued: " + taskInstance.getID());
    }

    protected Connection getConnection() {
        return this.connection;
    }

    protected TaskInstance getTaskInstance() {
        return this.taskInstance;
    }
}

import java.util.logging.Logger;

public class SubmitTaskHandler extends TaskHandler {

    private static final Logger LOGGER = Logger.getLogger(SubmitTaskHandler.class.getName());

    protected SubmitTaskHandler(Connection connection, TaskInstance taskInstance) {
        super(connection, taskInstance);
    }

    @Override
    protected String getTaskType() {
        return "SUBMIT_TASK";
    }

    @Override
    protected boolean isStateful() {
        return false;
    }

    @Override
    protected TaskStatus getCompletionStatus() {
        return TaskStatus.SUBMITTED;
    }

    @Override
    protected void execute(Connection connection) {
        LOGGER.info("Executing SubmitTaskHandler: " + getTaskInstance().getID());
    }
}

import java.util.logging.Logger;

public class HTTPTaskHandler extends TaskHandler {

    private static final Logger LOGGER = Logger.getLogger(HTTPTaskHandler.class.getName());

    protected HTTPTaskHandler(Connection connection, TaskInstance taskInstance) {
        super(connection, taskInstance);
    }

    @Override
    protected String getTaskType() {
        return "HTTP_TASK";
    }

    @Override
    protected boolean isStateful() {
        return true;
    }

    @Override
    protected TaskStatus getCompletionStatus() {
        return TaskStatus.API_CALL_COMPLETE;
    }

    @Override
    protected void execute(Connection connection) {
        LOGGER.info("Executing HTTPTaskHandler: " + getTaskInstance().getID());
        String url = getTaskInstance().getInputData().get("URL");
        // Perform HTTP request (dummy for now)
        LOGGER.info("Calling external API: " + url);
        completeAndClose(connection);
    }
}

#### Important design consideration for Workflow State Manager which is the core class that transitions a workflow forward based on its current status and figuring out task group or task level handling and if task group level check if task level then triggering the task handler for the next task etc. Handler base class should have a constructor(connection and taskinstance) —>  assign the task instance to class variable and call start method with connection object and it should create a new DAO object using connection to update the task status to in progress . A execute method which will only have access to task instance class variable, this has to be overridden by specific handlers like todo task handler , http task handler, task group handler etc, a cleanup(connection), which will also be overridden by specific task handlers to perform cleanups, getTaskType has to be overridden for sure etc. 

If we are smart about it, any api we expose is  through the methods which accept a connection object. Which can be passed down to services and services can create DAO objects using the connection or task handlers for constructor and start () 

We can add the Execution Queuing Interceptor to every handler close () method. Thereby whenever a Task completion is triggered through the Service —> TaskHandler completeAndClose() —> close() we use same connection object to insert the workflow execution id in queue table. If connection gets committed by host allocation, the current task transitions and the workflow gets queued as well. Otherwise as it should, it will not do task completion or queuing up workflow execution if for state management. 

We can write our state manager on how to progress a workflow. with how to handle block tasks or individual tasks completion. This is where the handler factory will be called to get specific handler like task group or todo task handler or submit etc. and a static method on handler will be called which calls constructor internally. 

Example flow idea, host application calls these APIs with connection object
1 workflow definition api - create workflow
2 workflow execution api - run workflow
2.1 workflow execution service uses the connection object to create DAOs to insert all the workflow executions block execution task executions etc with same connection object and call decider service with workflow instance id. 
2.2 decider service calls task handler factory to get the workflows first task type specific task handler static initializer method initAndStart(connection, task instance object). 
2.3 the base task handler have the run(connection, task instance object) gets the appropriate task handler (constructor initialized from handler factory) and call another method prepare(connection) moveToInProgress(connection) which transitions the task to in progress using connection object and creating a DAO as needed and finally run() return the object of the handler. If needed the task specific handlers can override prepare() but mostly not required. Also we will have methods for execute() and cleanup(connection) which similar to prepare() can be overridden, close(connection) can not be overridden in sub classes. Let’s not allow overriding run(). The methods prepare(), cleanup() and close() should not be externally callable. The run() —> prepare() —> moveToInProgress() —> execute() is one flow. The completeAndClose —> moveToComplete() —> cleanup() —> close() is another flow possible through handler. 

— Example flow section incompletely ended
<<Incomplete Example, just for context >>

### Important: No need to consider user and groups persistence as they will be injected by host planning application which will use this library. Maybe a few dto if required for the details we need to capture within tasks and workflows would be sufficient. And since they are not common, they should probably go into json part of db persistence but of course can be queried using our query service which utilizes the json based select query logic. 

Let’s use the group as com.workday and we can call the project planning-workflow-engine and for package we can append .pwe to refer as planning-workflow-engine. Let’s write everything file by file in the proper project structure. Also remember to keep updating todo as we go at each step and per instead of phase. This is very important otherwise we lose track of our progress if something breaks. 
