package com.example.tubemindai.models;

/**
 * Model class for Chat messages
 */
public class ChatModel {
    public static final int TYPE_USER = 1;
    public static final int TYPE_AI = 2;

    private String message;
    private int type; // 1 for user, 2 for AI
    private String timestamp;
    private String videoId;

    public ChatModel() {
    }

    public ChatModel(String message, int type, String timestamp, String videoId) {
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.videoId = videoId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }
}

