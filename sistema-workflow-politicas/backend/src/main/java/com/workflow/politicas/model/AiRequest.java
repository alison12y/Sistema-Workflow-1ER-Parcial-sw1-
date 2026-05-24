package com.workflow.politicas.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "ai_requests")
public class AiRequest {
    @Id
    private String id;
    private String userId;
    private String prompt;
    private String response;
    private String contextType; // "WORKFLOW_GENERATION", "FORM_ASSISTANCE"
    private LocalDateTime timestamp;

    public AiRequest() {
    }

    public AiRequest(String id, String userId, String prompt, String response, String contextType, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.prompt = prompt;
        this.response = response;
        this.contextType = contextType;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getContextType() {
        return contextType;
    }

    public void setContextType(String contextType) {
        this.contextType = contextType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
