package com.example.tubemindai.api.models;

public class OtpVerifyResponse {
    private String message;
    private boolean verified;
    private String token;
    private String reset_token;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getResetToken() {
        return reset_token;
    }

    public void setResetToken(String reset_token) {
        this.reset_token = reset_token;
    }
}

