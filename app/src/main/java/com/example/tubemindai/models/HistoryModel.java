package com.example.tubemindai.models;

/**
 * Model class for History items (Chat History, Saved Notes)
 */
public class HistoryModel {
    public static final int TYPE_CHAT = 1;
    public static final int TYPE_NOTES = 2;

    private String historyId;
    private int type;
    private String title;
    private String preview;
    private String date;
    private String videoId;
    private int videoDbId; // Database video ID for API calls

    public HistoryModel() {
    }

    public HistoryModel(String historyId, int type, String title, String preview, String date, String videoId) {
        this.historyId = historyId;
        this.type = type;
        this.title = title;
        this.preview = preview;
        this.date = date;
        this.videoId = videoId;
    }
    
    public HistoryModel(String historyId, int type, String title, String preview, String date, String videoId, int videoDbId) {
        this.historyId = historyId;
        this.type = type;
        this.title = title;
        this.preview = preview;
        this.date = date;
        this.videoId = videoId;
        this.videoDbId = videoDbId;
    }

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public int getVideoDbId() {
        return videoDbId;
    }

    public void setVideoDbId(int videoDbId) {
        this.videoDbId = videoDbId;
    }
}

