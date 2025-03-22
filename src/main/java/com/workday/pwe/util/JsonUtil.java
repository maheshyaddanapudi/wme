package com.workday.pwe.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for JSON operations.
 */
public class JsonUtil {

    private static final Logger LOGGER = Logger.getLogger(JsonUtil.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    /**
     * Private constructor to prevent instantiation
     */
    private JsonUtil() {
        // Do not instantiate
    }
    
    /**
     * Convert an object to a JsonNode
     */
    public static JsonNode toJsonNode(Object obj) {
        return OBJECT_MAPPER.valueToTree(obj);
    }
    
    /**
     * Convert a string to a JsonNode
     */
    public static JsonNode fromString(String json) {
        try {
            return json != null ? OBJECT_MAPPER.readTree(json) : null;
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Error parsing JSON string", e);
            throw new RuntimeException("Error parsing JSON string", e);
        }
    }
    
    /**
     * Convert a JsonNode to a string
     */
    public static String toString(JsonNode node) {
        try {
            return node != null ? OBJECT_MAPPER.writeValueAsString(node) : null;
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Error converting JsonNode to string", e);
            throw new RuntimeException("Error converting JsonNode to string", e);
        }
    }
    
    /**
     * Get a string value from a JsonNode at the specified path
     */
    public static String getStringValue(JsonNode node, String path) {
        return getStringValue(node, path, null);
    }
    
    /**
     * Get a string value from a JsonNode at the specified path with a default value
     */
    public static String getStringValue(JsonNode node, String path, String defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        
        String[] pathParts = path.split("\\.");
        JsonNode current = node;
        
        for (String part : pathParts) {
            current = current.get(part);
            if (current == null) {
                return defaultValue;
            }
        }
        
        return current.isTextual() ? current.asText() : defaultValue;
    }
    
    /**
     * Create a new ObjectNode
     */
    public static ObjectNode createObjectNode() {
        return OBJECT_MAPPER.createObjectNode();
    }
    
    /**
     * Merge two JsonNodes (shallow merge)
     */
    public static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {
        if (mainNode == null) {
            return updateNode;
        }
        if (updateNode == null) {
            return mainNode;
        }
        
        ObjectNode result = ((ObjectNode) mainNode).deepCopy();
        updateNode.fieldNames().forEachRemaining(fieldName -> 
            result.set(fieldName, updateNode.get(fieldName)));
        
        return result;
    }
}