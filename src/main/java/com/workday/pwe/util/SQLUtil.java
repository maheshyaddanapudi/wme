package com.workday.pwe.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for SQL operations.
 */
public class SQLUtil {

    private static final Logger LOGGER = Logger.getLogger(SQLUtil.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    /**
     * Private constructor to prevent instantiation
     */
    private SQLUtil() {
        // Do not instantiate
    }
    
    /**
     * Build a WHERE clause for a JSON field search
     * 
     * @param fieldName The JSON field name
     * @param jsonPath The path within the JSON
     * @param operator The operator (=, >, <, etc.)
     * @param value The value to compare against
     * @return The WHERE clause
     */
    public static String buildJsonSearch(String fieldName, String jsonPath, String operator, String value) {
        // The structure depends on the database type
        // This implementation is for PostgreSQL
        if (jsonPath == null || jsonPath.isEmpty()) {
            return fieldName + " " + operator + " '" + value + "'::jsonb";
        } else {
            return fieldName + "->'" + jsonPath + "' " + operator + " '" + value + "'::jsonb";
        }
    }
    
    /**
     * Build a WHERE clause with parameters
     * 
     * @param conditions List of conditions
     * @param values List of values
     * @return The WHERE clause
     */
    public static String buildWhereClause(List<String> conditions, List<Object> values) {
        if (conditions.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder("WHERE ");
        
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) {
                sb.append(" AND ");
            }
            
            String condition = conditions.get(i);
            if (values.get(i) == null) {
                // Handle NULL values
                if (condition.contains("=")) {
                    condition = condition.replace("= ?", "IS NULL");
                } else if (condition.contains("<>") || condition.contains("!=")) {
                    condition = condition.replace("<> ?", "IS NOT NULL").replace("!= ?", "IS NOT NULL");
                }
            }
            
            sb.append(condition);
        }
        
        return sb.toString();
    }
    
    /**
     * Set parameters for a prepared statement
     * 
     * @param stmt The prepared statement
     * @param values The parameter values
     * @throws SQLException If a database error occurs
     */
    public static void setParameters(PreparedStatement stmt, List<Object> values) throws SQLException {
        int paramIndex = 1;
        
        for (Object value : values) {
            if (value == null) {
                continue; // Skip NULL values (they're handled in the WHERE clause)
            } else if (value instanceof String) {
                stmt.setString(paramIndex++, (String) value);
            } else if (value instanceof Integer) {
                stmt.setInt(paramIndex++, (Integer) value);
            } else if (value instanceof Long) {
                stmt.setLong(paramIndex++, (Long) value);
            } else if (value instanceof Double) {
                stmt.setDouble(paramIndex++, (Double) value);
            } else if (value instanceof Boolean) {
                stmt.setBoolean(paramIndex++, (Boolean) value);
            } else if (value instanceof UUID) {
                stmt.setObject(paramIndex++, value);
            } else if (value instanceof java.time.LocalDateTime) {
                stmt.setTimestamp(paramIndex++, java.sql.Timestamp.valueOf((java.time.LocalDateTime) value));
            } else if (value instanceof JsonNode) {
                stmt.setString(paramIndex++, value.toString());
            } else {
                stmt.setObject(paramIndex++, value);
            }
        }
    }
    
    /**
     * Create an IN clause for a list of values
     * 
     * @param fieldName The field name
     * @param values The values
     * @return The IN clause
     */
    public static String createInClause(String fieldName, List<?> values) {
        if (values == null || values.isEmpty()) {
            return "1=0"; // Always false
        }
        
        StringBuilder sb = new StringBuilder(fieldName);
        sb.append(" IN (");
        
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("?");
        }
        
        sb.append(")");
        
        return sb.toString();
    }
    
    /**
     * Get a JSON path value from a result set
     * 
     * @param rs The result set
     * @param columnLabel The column label
     * @param jsonPath The path within the JSON
     * @return The value at the JSON path
     * @throws SQLException If a database error occurs
     */
    public static String getJsonValue(ResultSet rs, String columnLabel, String jsonPath) throws SQLException {
        String jsonStr = rs.getString(columnLabel);
        if (jsonStr == null) {
            return null;
        }
        
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonStr);
            String[] pathParts = jsonPath.split("\\.");
            
            for (String part : pathParts) {
                if (jsonNode == null) {
                    return null;
                }
                jsonNode = jsonNode.get(part);
            }
            
            return jsonNode != null ? jsonNode.asText() : null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing JSON", e);
            throw new SQLException("Error parsing JSON", e);
        }
    }
    
    /**
     * Build a complex query with pagination
     * 
     * @param baseQuery The base query
     * @param whereClause The WHERE clause
     * @param orderByClause The ORDER BY clause
     * @param limit The maximum number of results
     * @param offset The offset for pagination
     * @return The complete query
     */
    public static String buildPaginatedQuery(String baseQuery, String whereClause, 
                                         String orderByClause, int limit, int offset) {
        StringBuilder sb = new StringBuilder(baseQuery);
        
        if (whereClause != null && !whereClause.isEmpty()) {
            sb.append(" ").append(whereClause);
        }
        
        if (orderByClause != null && !orderByClause.isEmpty()) {
            sb.append(" ORDER BY ").append(orderByClause);
        }
        
        if (limit > 0) {
            sb.append(" LIMIT ").append(limit);
        }
        
        if (offset > 0) {
            sb.append(" OFFSET ").append(offset);
        }
        
        return sb.toString();
    }
    
    /**
     * Build a batch insert statement
     * 
     * @param tableName The table name
     * @param columns The column names
     * @param batchSize The number of rows to insert
     * @return The batch insert statement
     */
    public static String buildBatchInsert(String tableName, List<String> columns, int batchSize) {
        StringBuilder sb = new StringBuilder("INSERT INTO ");
        sb.append(tableName).append(" (");
        
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(columns.get(i));
        }
        
        sb.append(") VALUES ");
        
        for (int i = 0; i < batchSize; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            
            sb.append("(");
            for (int j = 0; j < columns.size(); j++) {
                if (j > 0) {
                    sb.append(", ");
                }
                sb.append("?");
            }
            sb.append(")");
        }
        
        return sb.toString();
    }
    
    /**
     * Get a count from a query
     * 
     * @param connection The database connection
     * @param countQuery The count query
     * @return The count
     * @throws SQLException If a database error occurs
     */
    public static int getCount(Connection connection, String countQuery) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(countQuery);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting count", e);
            throw e;
        }
    }
    
    /**
     * Execute a query and get a list of objects
     * 
     * @param <T> The type of objects to return
     * @param connection The database connection
     * @param query The query
     * @param mapper A function to map result set rows to objects
     * @return List of objects
     * @throws SQLException If a database error occurs
     */
    public static <T> List<T> executeQuery(Connection connection, String query, 
                                      java.util.function.Function<ResultSet, T> mapper) throws SQLException {
        List<T> results = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                results.add(mapper.apply(rs));
            }
            
            return results;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error executing query", e);
            throw e;
        }
    }
}
