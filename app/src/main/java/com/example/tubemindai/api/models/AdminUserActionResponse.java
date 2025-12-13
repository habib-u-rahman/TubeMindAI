package com.example.tubemindai.api.models;

public class AdminUserActionResponse {
    private String message;
    private int user_id;
    private boolean is_active;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getUserId() {
        return user_id;
    }

    public void setUserId(int user_id) {
        this.user_id = user_id;
    }

    public boolean isActive() {
        return is_active;
    }

    public void setActive(boolean is_active) {
        this.is_active = is_active;
    }
}

