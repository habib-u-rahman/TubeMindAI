package com.example.tubemindai.api.models;

public class LoginResponse {
    private String access_token;
    private String token_type;
    private int user_id;
    private String email;
    private String name;
    private boolean is_admin;

    public String getAccessToken() {
        return access_token;
    }

    public void setAccessToken(String access_token) {
        this.access_token = access_token;
    }

    public String getTokenType() {
        return token_type;
    }

    public void setTokenType(String token_type) {
        this.token_type = token_type;
    }

    public int getUserId() {
        return user_id;
    }

    public void setUserId(int user_id) {
        this.user_id = user_id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAdmin() {
        return is_admin;
    }

    public void setAdmin(boolean is_admin) {
        this.is_admin = is_admin;
    }
}

