package com.example.tubemindai.api.models;

public class PDFChatMessageRequest {
    private String message;

    public PDFChatMessageRequest(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

