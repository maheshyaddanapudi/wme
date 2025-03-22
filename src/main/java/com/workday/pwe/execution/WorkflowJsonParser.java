package com.workday.pwe.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.workday.pwe.dao.TaskDefinitionDAO;
import com.workday.pwe.dao.TaskGroupDefinitionDAO;
import com.workday.pwe.dao.WorkflowDefinitionDAO;
import com.workday.pwe.enums.CompletionCriteria;
import com.workday.pwe.enums.TaskGroupType;
import com.workday.pwe.enums.TaskType;
import com.workday.pwe.model.TaskDefinition;
import com.workday.pwe.model.TaskGroupDefinition;
import com.workday.pwe.model.WorkflowDefinition;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses JSON workflow definitions and creates the corresponding database entities.
 */
public class WorkflowJsonParser {

    private static final Logger LOGGER = Logger.getLogger(WorkflowJsonParser.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Private constructor to prevent instantiation
     */
    private WorkflowJsonParser() {
        // Do not instantiate
    }

    /**
     * Parse a JSON workflow definition
     * 
     * @param connection Database connection
     * @param workflowName The workflow name
     * @param jsonDefinition The JSON workflow definition
     * @return The created workflow definition
     */
    public static WorkflowDefinition parseWorkflowDefinition(Connection connection, String workflowName, String jsonDefinition) {
        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(jsonDefinition);
            
            // Create the workflow definition
            WorkflowDefinitionDAO workflowDAO = new WorkflowDefinitionDAO(connection);
            TaskGroupDefinitionDAO groupDAO = new TaskGroupDefinitionDAO(connection);
            TaskDefinitionDAO taskDAO = new TaskDefinitionDAO(connection);
            
            // Create workflow definition
            WorkflowDefinition workflowDef = new WorkflowDefinition(workflowName, rootNode);
            UUID workflowId = workflowDAO.createWorkflowDefinition(workflowDef);
            workflowDef.setId(workflowId);
            
            // Parse and create task groups and tasks
            parseNode(connection, rootNode, workflowId, null);
            
            return workflowDef;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing workflow definition", e);
            throw new RuntimeException("Error parsing workflow definition", e);
        }
    }
    
    /**
     * Parse a node in the workflow definition recursively
     * 
     * @param connection Database connection
     * @param node The JSON node
     * @param workflowId The workflow definition ID
     * @param parentGroupId The parent group ID (null for root nodes)
     * @return The UUID of the created entity
     */
    private static UUID parseNode(Connection connection, JsonNode node, UUID workflowId, UUID parentGroupId) {
        try {
            String nodeType = node.path("type").asText();
            String nodeId = node.path("id").asText();
            
            if ("group".equals(nodeType)) {
                return parseGroup(connection, node, workflowId, parentGroupId);
            } else if ("task".equals(nodeType)) {
                return parseTask(connection, node, workflowId, parentGroupId);
            } else {
                throw new IllegalArgumentException("Unknown node type: " + nodeType);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing node", e);
            throw new RuntimeException("Error parsing node", e);
        }
    }
    
    /**
     * Parse a group node
     * 
     * @param connection Database connection
     * @param node The JSON node
     * @param workflowId The workflow definition ID
     * @param parentGroupId The parent group ID (null for root nodes)
     * @return The UUID of the created group
     */
    private static UUID parseGroup(Connection connection, JsonNode node, UUID workflowId, UUID parentGroupId) {
        try {
            String groupId = node.path("id").asText();
            String groupTypeName = node.path("groupType").asText();
            String completionCriteriaName = node.path("completionCriteria").asText("ALL");
            JsonNode children = node.path("children");
            
            // Determine group type
            TaskGroupType groupType = TaskGroupType.VERTICAL; // Default
            if ("horizontal".equalsIgnoreCase(groupTypeName)) {
                groupType = TaskGroupType.HORIZONTAL;
            }
            
            // Determine completion criteria
            CompletionCriteria completionCriteria = CompletionCriteria.ALL; // Default
            if ("ANY".equalsIgnoreCase(completionCriteriaName)) {
                completionCriteria = CompletionCriteria.ANY;
            } else if ("N_OF_M".equalsIgnoreCase(completionCriteriaName)) {
                completionCriteria = CompletionCriteria.N_OF_M;
            }
            
            // Create the task group definition
            TaskGroupDefinitionDAO groupDAO = new TaskGroupDefinitionDAO(connection);
            TaskGroupDefinition groupDef = new TaskGroupDefinition(workflowId, groupId, groupType, completionCriteria, 0);
            groupDef.setParentGroupDefId(parentGroupId);
            
            // Extract parameters from the JSON
            ObjectNode parametersJson = OBJECT_MAPPER.createObjectNode();
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (!List.of("id", "type", "groupType", "completionCriteria", "children").contains(fieldName)) {
                    parametersJson.set(fieldName, node.get(fieldName));
                }
            }
            groupDef.setParametersJson(parametersJson);
            
            // Create the group in the database
            UUID groupDefId = groupDAO.createTaskGroupDefinition(groupDef);
            
            // Process child nodes
            if (children != null && children.isArray()) {
                int childOrder = 0;
                for (JsonNode child : children) {
                    parseNode(connection, child, workflowId, groupDefId);
                    childOrder++;
                }
            }
            
            return groupDefId;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing group", e);
            throw new RuntimeException("Error parsing group", e);
        }
    }
    
    /**
     * Parse a task node
     * 
     * @param connection Database connection
     * @param node The JSON node
     * @param workflowId The workflow definition ID
     * @param parentGroupId The parent group ID (null for root nodes)
     * @return The UUID of the created task
     */
    private static UUID parseTask(Connection connection, JsonNode node, UUID workflowId, UUID parentGroupId) {
        try {
            String taskId = node.path("id").asText();
            String taskTypeName = node.path("taskType").asText();
            String assignee = node.path("assignee").asText();
            
            // Determine task type
            TaskType taskType;
            try {
                taskType = TaskType.valueOf(taskTypeName.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Unknown task type: " + taskTypeName + ", defaulting to TODO");
                taskType = TaskType.TODO;
            }
            
            // Create the task definition
            TaskDefinitionDAO taskDAO = new TaskDefinitionDAO(connection);
            TaskDefinition taskDef = new TaskDefinition(workflowId, taskId, taskType, 0);
            taskDef.setTaskGroupDefId(parentGroupId);
            
            // Extract parameters from the JSON
            ObjectNode parametersJson = OBJECT_MAPPER.createObjectNode();
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (!List.of("id", "type", "taskType").contains(fieldName)) {
                    parametersJson.set(fieldName, node.get(fieldName));
                }
            }
            taskDef.setParametersJson(parametersJson);
            
            // Create the task in the database
            UUID taskDefId = taskDAO.createTaskDefinition(taskDef);
            
            return taskDefId;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing task", e);
            throw new RuntimeException("Error parsing task", e);
        }
    }
    
    /**
     * Generate a JSON representation of a workflow definition
     * 
     * @param connection Database connection
     * @param workflowDefId The workflow definition ID
     * @return The JSON representation
     */
    public static JsonNode generateWorkflowJson(Connection connection, UUID workflowDefId) {
        try {
            WorkflowDefinitionDAO workflowDAO = new WorkflowDefinitionDAO(connection);
            TaskGroupDefinitionDAO groupDAO = new TaskGroupDefinitionDAO(connection);
            TaskDefinitionDAO taskDAO = new TaskDefinitionDAO(connection);
            
            // Get the workflow definition
            WorkflowDefinition workflowDef = workflowDAO.getWorkflowDefinition(workflowDefId);
            if (workflowDef == null) {
                throw new IllegalArgumentException("Workflow definition not found: " + workflowDefId);
            }
            
            // Get root task groups
            List<TaskGroupDefinition> rootGroups = groupDAO.getRootTaskGroups(workflowDefId);
            
            // Get top-level tasks (not in any group)
            List<TaskDefinition> topLevelTasks = taskDAO.getTopLevelTasks(workflowDefId);
            
            // Create the root node
            ObjectNode rootNode = OBJECT_MAPPER.createObjectNode();
            rootNode.put("id", "root");
            rootNode.put("type", "group");
            rootNode.put("groupType", "vertical");
            rootNode.put("completionCriteria", "ALL");
            
            // Add children array
            List<JsonNode> childNodes = new ArrayList<>();
            
            // Add root groups as children
            for (TaskGroupDefinition group : rootGroups) {
                childNodes.add(generateGroupJson(connection, group));
            }
            
            // Add top-level tasks as children
            for (TaskDefinition task : topLevelTasks) {
                childNodes.add(generateTaskJson(task));
            }
            
            rootNode.set("children", OBJECT_MAPPER.valueToTree(childNodes));
            
            return rootNode;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating workflow JSON", e);
            throw new RuntimeException("Error generating workflow JSON", e);
        }
    }
    
    /**
     * Generate a JSON representation of a task group
     * 
     * @param connection Database connection
     * @param group The task group definition
     * @return The JSON representation
     */
    private static JsonNode generateGroupJson(Connection connection, TaskGroupDefinition group) {
        try {
            TaskGroupDefinitionDAO groupDAO = new TaskGroupDefinitionDAO(connection);
            TaskDefinitionDAO taskDAO = new TaskDefinitionDAO(connection);
            
            // Create the group node
            ObjectNode groupNode = OBJECT_MAPPER.createObjectNode();
            groupNode.put("id", group.getName());
            groupNode.put("type", "group");
            groupNode.put("groupType", group.getGroupType().name().toLowerCase());
            groupNode.put("completionCriteria", group.getCompletionCriteria().name());
            
            // Copy parameters from the group's parametersJson
            if (group.getParametersJson() != null) {
                Iterator<String> fieldNames = group.getParametersJson().fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    groupNode.set(fieldName, group.getParametersJson().get(fieldName));
                }
            }
            
            // Get child groups and tasks
            List<TaskGroupDefinition> childGroups = groupDAO.getChildGroups(group.getId());
            List<TaskDefinition> childTasks = taskDAO.getTasksByGroupId(group.getId());
            
            // Add children array
            List<JsonNode> childNodes = new ArrayList<>();
            
            // Add child groups as children
            for (TaskGroupDefinition childGroup : childGroups) {
                childNodes.add(generateGroupJson(connection, childGroup));
            }
            
            // Add child tasks as children
            for (TaskDefinition childTask : childTasks) {
                childNodes.add(generateTaskJson(childTask));
            }
            
            groupNode.set("children", OBJECT_MAPPER.valueToTree(childNodes));
            
            return groupNode;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating group JSON", e);
            throw new RuntimeException("Error generating group JSON", e);
        }
    }
    
    /**
     * Generate a JSON representation of a task
     * 
     * @param task The task definition
     * @return The JSON representation
     */
    private static JsonNode generateTaskJson(TaskDefinition task) {
        try {
            // Create the task node
            ObjectNode taskNode = OBJECT_MAPPER.createObjectNode();
            taskNode.put("id", task.getName());
            taskNode.put("type", "task");
            taskNode.put("taskType", task.getTaskType().name());
            
            // Copy parameters from the task's parametersJson
            if (task.getParametersJson() != null) {
                Iterator<String> fieldNames = task.getParametersJson().fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    taskNode.set(fieldName, task.getParametersJson().get(fieldName));
                }
            }
            
            return taskNode;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating task JSON", e);
            throw new RuntimeException("Error generating task JSON", e);
        }
    }
}