package com.example.tubemindai.api.models;

import java.util.Date;

public class PDFChatMessageResponse {
    private int id;
    private String message;
    private String response;
    private boolean is_user_message;
    private Date created_at;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    public boolean isUserMessage() { return is_user_message; }
    public void setUserMessage(boolean is_user_message) { this.is_user_message = is_user_message; }
    public Date getCreatedAt() { return created_at; }
    public void setCreatedAt(Date created_at) { this.created_at = created_at; }
}

