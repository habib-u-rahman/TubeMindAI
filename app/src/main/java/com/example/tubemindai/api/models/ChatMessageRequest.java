package com.example.tubemindai.api.models;

public class ChatMessageRequest {
    private String message;

    public ChatMessageRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

