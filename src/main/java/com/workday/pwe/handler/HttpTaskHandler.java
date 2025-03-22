package com.workday.pwe.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.workday.pwe.enums.TaskStatus;
import com.workday.pwe.enums.TaskType;
import com.workday.pwe.model.TaskInstance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for HTTP tasks that make external API calls.
 */
public class HttpTaskHandler extends TaskHandler {

    private static final Logger LOGGER = Logger.getLogger(HttpTaskHandler.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Constructor with task instance
     * 
     * @param taskInstance The task instance to handle
     */
    protected HttpTaskHandler(TaskInstance taskInstance) {
        super(taskInstance);
    }

    @Override
    protected String getTaskType() {
        return TaskType.HTTP.name();
    }

    @Override
    protected boolean isStateful() {
        return true; // HTTP tasks are stateful
    }

    @Override
    protected TaskStatus getCompletionStatus() {
        return TaskStatus.API_CALL_COMPLETE; // Custom completion status for HTTP tasks
    }

    @Override
    protected void execute(Connection connection) {
        LOGGER.info("Executing HttpTaskHandler for task: " + getTaskInstance().getId());
        
        try {
            TaskInstance task = getTaskInstance();
            JsonNode inputJson = task.getInputJson();
            
            // Extract HTTP request parameters from input JSON
            if (inputJson == null) {
                failTask(connection, "Missing input parameters for HTTP task");
                return;
            }
            
            String url = getStringParam(inputJson, "url");
            String method = getStringParam(inputJson, "method", "GET");
            String body = getStringParam(inputJson, "body", null);
            int timeout = getIntParam(inputJson, "timeoutSeconds", 30);
            
            if (url == null || url.isEmpty()) {
                failTask(connection, "URL is required for HTTP task");
                return;
            }
            
            // Build the HTTP request
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeout))
                    .header("Content-Type", "application/json");
            
            // Set the appropriate HTTP method
            switch (method.toUpperCase()) {
                case "GET":
                    requestBuilder.GET();
                    break;
                case "POST":
                    if (body != null) {
                        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
                    } else {
                        requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
                    }
                    break;
                case "PUT":
                    if (body != null) {
                        requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body));
                    } else {
                        requestBuilder.PUT(HttpRequest.BodyPublishers.noBody());
                    }
                    break;
                case "DELETE":
                    requestBuilder.DELETE();
                    break;
                default:
                    failTask(connection, "Unsupported HTTP method: " + method);
                    return;
            }
            
            HttpRequest request = requestBuilder.build();
            
            // Execute the HTTP request
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Process the response
            int statusCode = response.statusCode();
            String responseBody = response.body();
            
            // Create output JSON with response data
            ObjectNode outputJson = OBJECT_MAPPER.createObjectNode();
            outputJson.put("statusCode", statusCode);
            outputJson.put("responseBody", responseBody);
            
            // Headers as a JSON object
            ObjectNode headersJson = outputJson.putObject("headers");
            response.headers().map().forEach((key, values) -> {
                if (values.size() == 1) {
                    headersJson.put(key, values.get(0));
                } else if (values.size() > 1) {
                    headersJson.putPOJO(key, values);
                }
            });
            
            if (statusCode >= 200 && statusCode < 300) {
                // Success case
                completeTask(connection, outputJson);
            } else {
                // Error case - we still complete the task but with error details
                // This allows the workflow to handle HTTP errors appropriately
                completeTask(connection, outputJson);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing HTTP task", e);
            failTask(connection, "Error executing HTTP task: " + e.getMessage());
        }
    }
    
    // Helper methods to extract parameters from input JSON
    
    private String getStringParam(JsonNode json, String paramName) {
        return getStringParam(json, paramName, null);
    }
    
    private String getStringParam(JsonNode json, String paramName, String defaultValue) {
        JsonNode node = json.get(paramName);
        return (node != null && !node.isNull()) ? node.asText() : defaultValue;
    }
    
    private int getIntParam(JsonNode json, String paramName, int defaultValue) {
        JsonNode node = json.get(paramName);
        return (node != null && !node.isNull()) ? node.asInt() : defaultValue;
    }
}