package com.example.tubemindai.models;

/**
 * Model class for User data
 */
public class UserModel {
    private String userId;
    private String name;
    private String email;
    private String profileImageUrl;

    public UserModel() {
    }

    public UserModel(String userId, String name, String email, String profileImageUrl) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}

