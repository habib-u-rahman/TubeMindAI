package com.example.tubemindai.api.models;

public class ChatMessageResponse {
    private int id;
    private String message;
    private String response;
    private boolean isUserMessage;
    private String createdAt;

    public ChatMessageResponse() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public boolean isUserMessage() {
        return isUserMessage;
    }

    public void setUserMessage(boolean userMessage) {
        isUserMessage = userMessage;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

