package com.example.tubemindai.api.models;

public class ResetPasswordRequest {
    private String email;
    private String new_password;
    private String confirm_password;
    private String reset_token;

    public ResetPasswordRequest(String email, String new_password, String confirm_password, String reset_token) {
        this.email = email;
        this.new_password = new_password;
        this.confirm_password = confirm_password;
        this.reset_token = reset_token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNewPassword() {
        return new_password;
    }

    public void setNewPassword(String new_password) {
        this.new_password = new_password;
    }

    public String getConfirmPassword() {
        return confirm_password;
    }

    public void setConfirmPassword(String confirm_password) {
        this.confirm_password = confirm_password;
    }

    public String getResetToken() {
        return reset_token;
    }

    public void setResetToken(String reset_token) {
        this.reset_token = reset_token;
    }
}

