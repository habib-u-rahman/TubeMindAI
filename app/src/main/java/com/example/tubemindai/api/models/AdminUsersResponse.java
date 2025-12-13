package com.example.tubemindai.api.models;

import java.util.List;

public class AdminUsersResponse {
    private List<AdminUserItem> users;
    private int total;
    private int skip;
    private int limit;

    public List<AdminUserItem> getUsers() {
        return users;
    }

    public void setUsers(List<AdminUserItem> users) {
        this.users = users;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSkip() {
        return skip;
    }

    public void setSkip(int skip) {
        this.skip = skip;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public static class AdminUserItem {
        private int id;
        private String name;
        private String email;
        private boolean is_active;
        private boolean is_verified;
        private boolean is_admin;
        private String created_at;
        private int video_count;
        private int chat_count;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public boolean isActive() { return is_active; }
        public void setActive(boolean is_active) { this.is_active = is_active; }
        public boolean isVerified() { return is_verified; }
        public void setVerified(boolean is_verified) { this.is_verified = is_verified; }
        public boolean isAdmin() { return is_admin; }
        public void setAdmin(boolean is_admin) { this.is_admin = is_admin; }
        public String getCreatedAt() { return created_at; }
        public void setCreatedAt(String created_at) { this.created_at = created_at; }
        public int getVideoCount() { return video_count; }
        public void setVideoCount(int video_count) { this.video_count = video_count; }
        public int getChatCount() { return chat_count; }
        public void setChatCount(int chat_count) { this.chat_count = chat_count; }
    }
}

